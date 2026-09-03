#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Crea en GitHub las épicas como milestones, las etiquetas y las 112 historias
como issues, leyendo historias.csv. Idempotente: si una issue con el mismo
título ya existe, la salta.

El token se lee, en este orden:
  1. --token-file RUTA
  2. la variable de entorno GITHUB_TOKEN
  3. un archivo .github-token en la raíz del repo   (está en .gitignore)

Se recomienda un token de acceso personal de grano fino, limitado a este
repositorio, con permiso "Issues: Read and write" y caducidad de un día.

  python3 historias/crear-issues.py --repo donaldoAndres1985/Regenta
  python3 historias/crear-issues.py --repo owner/repo --dry-run
"""
import argparse, csv, json, os, sys, time, urllib.request, urllib.error

API = 'https://api.github.com'
COLORES = {
    'fundacion': '6E4B1F', 'backend': '1D4E6B', 'flutter': '0C6473', 'bbdd': '4A5C6D',
    'seguridad': 'A0271B', 'eventos': '8A5B06', 'ci': '5E7183', 'infra': '5E7183',
    'clave': '9A5709', 'core': '4A5C6D', 'clientes': '0C6473', 'inventario': '9A5709',
    'ventas': '9A5709', 'compras': '9A5709', 'facturacion': '4A5C6D', 'caja': '4A5C6D',
    'recursos': '0C6473', 'reservas': '0C6473', 'menu': 'A03325', 'mesas': 'A03325',
    'comandas': 'A03325', 'alertas': '8A5B06', 'reportes': '4A5C6D', 'auditoria': '4A5C6D',
}
AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)


def token(ruta=None):
    if ruta:
        return open(ruta, encoding='utf-8').read().strip()
    if os.environ.get('GITHUB_TOKEN'):
        return os.environ['GITHUB_TOKEN'].strip()
    por_defecto = os.path.join(RAIZ, '.github-token')
    if os.path.exists(por_defecto):
        return open(por_defecto, encoding='utf-8').read().strip()
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
            return r.status, json.loads(r.read() or b'null')
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read() or b'null')
        except Exception:
            return e.code, None


def cuerpo_issue(f):
    L = ['**Como** %s, **quiero** %s **para** %s' % (f['Como'], f['Quiero'], f['Para']), '',
         '| | |', '|---|---|',
         '| Épica | `%s` · %s |' % (f['Epica'], f['EpicaNombre']),
         '| Puntos | %s |' % f['Puntos']]
    if f['Microservicio'] and f['Microservicio'] != '—':
        L.append('| Microservicio | `%s` |' % f['Microservicio'])
    if f['PaqueteFlutter'] and f['PaqueteFlutter'] != '—':
        L.append('| Paquete Flutter | `%s` |' % f['PaqueteFlutter'])
    if f['Tablas']:
        L.append('| Tablas | %s |' % ' · '.join('`%s`' % t.strip() for t in f['Tablas'].split(',')))
    if f['Pantalla'] and f['Pantalla'] != '—':
        L.append('| Pantalla | `%s` |' % f['Pantalla'])
    L.append('| Depende de | %s |' % (f['DependeDe'] or '—'))
    L.append('')
    if f['Notas']:
        L += ['> ' + f['Notas'].replace('\n', ' '), '']
    L += ['### Criterios de aceptación', '', f['CriteriosAceptacion'], '',
          '### Terminado cuando', '',
          '- [ ] Los criterios de aceptación pasan como tests automatizados.']
    if f['Microservicio'] and f['Microservicio'] != '—':
        L.append('- [ ] Endpoints en el contrato OpenAPI del servicio.')
    if f['Tablas']:
        L.append('- [ ] Migración Flyway aplicada.')
    if f['Pantalla'] and f['Pantalla'] != '—':
        L.append('- [ ] Coincide con `%s`.' % f['Pantalla'])
    L += ['- [ ] Revisada en PR por otra persona.', '',
          '_Detalle completo en `historias/epicas/`._']
    return '\n'.join(L)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--repo', default='donaldoAndres1985/Regenta')
    p.add_argument('--token-file')
    p.add_argument('--dry-run', action='store_true')
    a = p.parse_args()
    tk = token(a.token_file)

    st, yo = api(tk, 'GET', '/user')
    if st != 200:
        sys.exit('El token no sirve (%s): %s' % (st, (yo or {}).get('message')))
    print('Autenticado como: %s' % yo.get('login'))

    st, repo = api(tk, 'GET', '/repos/' + a.repo)
    if st != 200:
        sys.exit('No alcanzo el repo (%s): %s' % (st, (repo or {}).get('message')))
    if not repo.get('permissions', {}).get('push'):
        sys.exit('El token no tiene permiso de escritura sobre %s' % a.repo)
    print('Repositorio: %s\n' % repo['full_name'])

    filas = list(csv.DictReader(open(os.path.join(AQUI, 'historias.csv'), encoding='utf-8-sig')))
    print('Historias en el CSV: %d' % len(filas))
    if a.dry_run:
        print('\n-- SIMULACIÓN, no se crea nada --')

    # ---- etiquetas
    print('\n== Etiquetas ==')
    etiquetas = sorted({e.strip() for f in filas for e in f['Etiquetas'].split(',') if e.strip()})
    for e in etiquetas:
        if a.dry_run:
            print('  (simulado) %s' % e); continue
        st, _ = api(tk, 'POST', '/repos/%s/labels' % a.repo,
                    {'name': e, 'color': COLORES.get(e, '888888')})
        print('  %-14s %s' % (e, 'creada' if st == 201 else ('ya existía' if st == 422 else 'error %s' % st)))

    # ---- milestones
    print('\n== Hitos ==')
    st, existentes = api(tk, 'GET', '/repos/%s/milestones' % a.repo, params='?state=all&per_page=100')
    por_titulo = {m['title']: m['number'] for m in (existentes or [])}
    vistos, orden = {}, []
    for f in filas:
        t = '%s · %s' % (f['Epica'], f['EpicaNombre'])
        if t not in vistos:
            vistos[t] = True; orden.append(t)
    for t in orden:
        if t in por_titulo:
            print('  %-46s ya existía (#%d)' % (t, por_titulo[t])); continue
        if a.dry_run:
            print('  (simulado) %s' % t); continue
        st, m = api(tk, 'POST', '/repos/%s/milestones' % a.repo, {'title': t})
        if st == 201:
            por_titulo[t] = m['number']; print('  %-46s creado (#%d)' % (t, m['number']))
        else:
            print('  %-46s error %s' % (t, st))

    # ---- issues (idempotente por título)
    print('\n== Historias ==')
    abiertas = {}
    pag = 1
    while True:
        st, lote = api(tk, 'GET', '/repos/%s/issues' % a.repo,
                       params='?state=all&per_page=100&page=%d' % pag)
        if st != 200 or not lote:
            break
        for i in lote:
            abiertas[i['title']] = i['number']
        if len(lote) < 100:
            break
        pag += 1
    print('  issues ya existentes en el repo: %d\n' % len(abiertas))

    creadas = saltadas = fallidas = 0
    for f in filas:
        titulo = '%s · %s' % (f['ID'], f['Titulo'])
        if titulo in abiertas:
            saltadas += 1; print('  = %s (ya existe #%d)' % (f['ID'], abiertas[titulo])); continue
        if a.dry_run:
            creadas += 1; print('  + %s (simulado)' % f['ID']); continue
        cuerpo = {
            'title': titulo,
            'body': cuerpo_issue(f),
            'labels': [e.strip() for e in f['Etiquetas'].split(',') if e.strip()],
        }
        ms = por_titulo.get('%s · %s' % (f['Epica'], f['EpicaNombre']))
        if ms:
            cuerpo['milestone'] = ms
        st, r = api(tk, 'POST', '/repos/%s/issues' % a.repo, cuerpo)
        if st == 201:
            creadas += 1; print('  + %s  ->  #%d' % (f['ID'], r['number']))
        else:
            fallidas += 1; print('  ! %s  error %s: %s' % (f['ID'], st, (r or {}).get('message')))
            if st in (403, 429):
                print('    límite de tasa; espero 60 s'); time.sleep(60)
        time.sleep(1.2)          # el límite de creación de contenido es ~80/min

    print('\nCreadas: %d · ya existían: %d · fallidas: %d' % (creadas, saltadas, fallidas))


if __name__ == '__main__':
    main()
