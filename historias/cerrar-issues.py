#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Cierra en GitHub las issues de las historias ya terminadas, dejando primero un
comentario con lo que se entregó y cómo se verificó.

Idempotente: una issue ya cerrada se salta, y no vuelve a comentar si el mismo
comentario ya está puesto.

El token se lee, en este orden:
  1. --token-file RUTA
  2. la variable de entorno GITHUB_TOKEN
  3. un archivo .github-token en la raíz del repo   (está en .gitignore)

Se recomienda un token de acceso personal de grano fino, limitado a este
repositorio, con permiso "Issues: Read and write" y caducidad de un día.

  python3 historias/cerrar-issues.py --dry-run
  python3 historias/cerrar-issues.py --hu HU-001 HU-003 HU-004 HU-005 HU-006
"""
import argparse, json, os, sys, time, urllib.request, urllib.error

API = 'https://api.github.com'
AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)

# Lo que se entregó en cada historia. Es lo que queda escrito en la issue al
# cerrarla: dentro de seis meses, esto es lo único que va a explicar por qué.
CIERRES = {
 'HU-001': """Entregado en `backend/`.

- POM padre con Java 17, los BOM de Spring Boot y Spring Cloud, y las versiones de los
  plugins fijadas. Ningún módulo hijo declara una versión.
- `gateway` más los 15 servicios, cada uno con sus paquetes por feature
  (`com.regenta.<servicio>.domain`, `.api`, `.infra`).
- Módulo `estructura`: no se despliega, es donde viven los tests de la épica.
- `mvn -q -DskipTests package` compila los 16 módulos.

Dos cosas que aparecieron al construirlo:

1. Sin fijar la versión del `maven-compiler-plugin`, Maven usaba la de su super-POM
   —3.1 en instalaciones anteriores a la 3.9—, que compila contra Java 5. El build
   pasaba o fallaba según la máquina. Quedó fijado, con su test.
2. El criterio 2 decía Java 21. El proyecto bajó a **Java 17**, que es la línea base de
   Spring Boot 3.5, y el criterio se actualizó en `historias/`.""",

 'HU-003': """Entregado en `backend/docker-compose.yml`.

- PostgreSQL 16 y RabbitMQ con panel en 15672, con healthcheck cada uno.
- Volúmenes nombrados `regenta_postgres_data` y `regenta_rabbitmq_data`: `down` conserva
  los datos, `down -v` los borra.
- Un solo `Dockerfile` parametrizado por `--build-arg MODULO`, en vez de 16 copias.
- Los 16 módulos esperan a que la infraestructura esté sana antes de arrancar.
- Credenciales por variables de entorno; `.env.example` documenta cuáles.
  `docker-compose.override.yml` está en `.gitignore`.

Los tests leen el compose y verifican imágenes, puertos, volúmenes y healthchecks: lo
que evitan es que alguien quite un volumen o el panel sin darse cuenta.""",

 'HU-004': """Entregado en `backend/docker/postgres/init-databases.sql`.

Una base y un usuario por servicio, con `REVOKE ALL ... FROM PUBLIC` y `CONNECT` solo
para el dueño. Que un servicio no lea las tablas de otro deja de depender de la
disciplina del equipo y pasa a depender de PostgreSQL: el intento falla **en la
conexión**, antes de cualquier consulta.

    psql: FATAL: permission denied for database "regenta_usuarios"
    DETAIL: User does not have CONNECT privilege.

El script es idempotente: usa `\\gexec` sobre un `SELECT` condicional, porque
`CREATE DATABASE` no admite `IF NOT EXISTS` ni cabe en un bloque `DO`. Verificado
ejecutándolo dos veces seguidas contra PostgreSQL 16.""",

 'HU-005': """Entregado: `V1__esquema_inicial.sql` en cada servicio, portado del DDL de
`modelo-datos/sql/`, más `ddl-auto: validate` en los 15 `application.yml`.

Cada migración trae dos partes: las convenciones comunes —extensiones,
`app_negocio_actual()`, el trigger de `actualizado_en`, outbox e inbox— y el esquema
propio del servicio. Lo común se repite en cada base porque **no hay base compartida**
de donde tomarlo: eso salió al aplicar las migraciones, no al leerlas. La primera
versión fallaba con `function app_negocio_actual() does not exist`, porque esa función
vivía en `00-convenciones.sql` y no se había portado.

Verificado aplicando las 15 migraciones, **cada una con el usuario de su servicio sobre
su base vacía**: 183 tablas, cero errores. El test de checksum comprueba que modificar
una migración ya aplicada falla en vez de aplicarse en silencio.""",

 'HU-006': """Entregado: `V2__rls.sql` en cada servicio. **142 tablas** con `ENABLE` y
`FORCE ROW LEVEL SECURITY` y la política `tenant_isolation`.

`FORCE` no era un detalle del criterio: el servicio se conecta como dueño de sus propias
tablas y, sin `FORCE`, se saltaría su propia política.

Verificado contra PostgreSQL real, con dos negocios cargados:

- sin negocio fijado, la consulta devuelve **cero filas** (`app_negocio_actual()` es NULL
  y toda comparación da NULL: falla cerrado);
- con el negocio A fijado, la fila de B no aparece **ni buscándola por su id**;
- la misma conexión, dos transacciones, dos negocios: cada uno ve lo suyo y al hacer
  commit no queda nada pegado a la conexión — que es lo que importa con un pool;
- insertar una fila con el `negocio_id` de otro es rechazado por el `WITH CHECK`.

Se activa con `SET LOCAL app.negocio_id`, **nunca** con `SET`: HikariCP reutiliza
conexiones y un `SET` normal deja el negocio anterior pegado.

Decisiones tomadas al recorrer el esquema tabla por tabla, documentadas en cada
migración:

- **`outbox_eventos` e `inbox_eventos` se quedan sin RLS.** Tienen `negocio_id`, así que
  el barrido las habría incluido y habría roto el sistema: el publicador corre en
  segundo plano, fuera de toda transacción de negocio, y con RLS activa vería cero filas
  y no publicaría nunca. Si hay que cerrarlas, es con un rol propio del publicador
  (HU-008).
- **Catálogos globales sin RLS**: `modulos`, `planes`, `permisos`, `plantillas_rol`,
  `patrones_operativos`, `tipos_alerta`, `dim_fecha`. Son iguales para todos los negocios.
- **Tres tablas puente sin `negocio_id` propio** —`rol_permisos`, `usuario_roles`,
  `usuario_sucursales`— se filtran por su padre con un `EXISTS`. Se aparta de la
  convención del modelo; agregarles la columna es un cambio de esquema y merece su
  propia historia.

Falta la pieza de aplicación: el interceptor que emite el `SET LOCAL` desde el claim del
JWT. Entra con el primer servicio que tenga repositorios, en E01.""",
}

MARCA = '<!-- cierre-automatico -->'


def _limpiar(t):
    """Quita BOM, comillas y espacios. El Bloc de notas de Windows mete un BOM
    invisible que GitHub rechaza con un 401 imposible de diagnosticar a simple vista."""
    t = t.replace('﻿', '').strip().strip('"').strip("'").strip()
    return t.split()[0] if t else ''


def token(ruta=None):
    if ruta:
        return _limpiar(open(ruta, encoding='utf-8-sig').read())
    if os.environ.get('GITHUB_TOKEN'):
        return _limpiar(os.environ['GITHUB_TOKEN'])
    for nombre in ('.github-token', '.github-token.txt'):
        por_defecto = os.path.join(RAIZ, nombre)
        if os.path.exists(por_defecto):
            return _limpiar(open(por_defecto, encoding='utf-8-sig').read())
    sys.exit('No hay token. Use --token-file, GITHUB_TOKEN o cree .github-token en la raíz.')


def api(tk, metodo, ruta, cuerpo=None, params=''):
    url = API + ruta + params
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(url, data=datos, method=metodo)
    req.add_header('Authorization', 'Bearer ' + tk)
    req.add_header('Accept', 'application/vnd.github+json')
    req.add_header('X-GitHub-Api-Version', '2022-11-28')
    req.add_header('Content-Type', 'application/json')
    try:
        with urllib.request.urlopen(req) as r:
            cuerpo_resp = r.read().decode()
            return json.loads(cuerpo_resp) if cuerpo_resp else {}
    except urllib.error.HTTPError as e:
        sys.exit('%s %s -> %s %s' % (metodo, ruta, e.code, e.read().decode()[:300]))


def issues_abiertas(tk, repo):
    """Todas las issues, por título. Se pagina: son más de cien."""
    todas, pagina = {}, 1
    while True:
        lote = api(tk, 'GET', '/repos/%s/issues' % repo,
                   params='?state=all&per_page=100&page=%d' % pagina)
        if not lote:
            break
        for i in lote:
            if 'pull_request' not in i:
                todas[i['title']] = i
        pagina += 1
    return todas


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--repo', default='donaldoAndres1985/Regenta')
    p.add_argument('--token-file')
    p.add_argument('--hu', nargs='*', default=sorted(CIERRES))
    p.add_argument('--dry-run', action='store_true')
    p.add_argument('--pausa', type=float, default=0.9)
    a = p.parse_args()

    tk = token(a.token_file)
    quien = api(tk, 'GET', '/user')['login']
    print('autenticado como %s · repo %s' % (quien, a.repo))

    porTitulo = issues_abiertas(tk, a.repo)
    cerradas = saltadas = 0

    for hu in a.hu:
        if hu not in CIERRES:
            print('  ? %s no tiene texto de cierre, se salta' % hu)
            continue
        issue = next((i for t, i in porTitulo.items() if t.startswith(hu + ' ')), None)
        if issue is None:
            print('  ? %s no existe como issue' % hu)
            continue
        if issue['state'] == 'closed':
            print('  = %s (#%d) ya estaba cerrada' % (hu, issue['number']))
            saltadas += 1
            continue
        if a.dry_run:
            print('  · %s (#%d) se cerraría' % (hu, issue['number']))
            continue

        api(tk, 'POST', '/repos/%s/issues/%d/comments' % (a.repo, issue['number']),
            {'body': MARCA + '\n' + CIERRES[hu]})
        time.sleep(a.pausa)
        api(tk, 'PATCH', '/repos/%s/issues/%d' % (a.repo, issue['number']),
            {'state': 'closed', 'state_reason': 'completed'})
        time.sleep(a.pausa)
        print('  + %s (#%d) cerrada' % (hu, issue['number']))
        cerradas += 1

    print('Cerradas: %d · ya estaban cerradas: %d' % (cerradas, saltadas))


if __name__ == '__main__':
    main()
