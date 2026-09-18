// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'base_local.dart';

// ignore_for_file: type=lint
class $CatalogosTable extends Catalogos
    with TableInfo<$CatalogosTable, Catalogo> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $CatalogosTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _claveMeta = const VerificationMeta('clave');
  @override
  late final GeneratedColumn<String> clave = GeneratedColumn<String>(
    'clave',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _tipoMeta = const VerificationMeta('tipo');
  @override
  late final GeneratedColumn<String> tipo = GeneratedColumn<String>(
    'tipo',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
    defaultValue: const Constant('generico'),
  );
  static const VerificationMeta _contenidoMeta = const VerificationMeta(
    'contenido',
  );
  @override
  late final GeneratedColumn<String> contenido = GeneratedColumn<String>(
    'contenido',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _actualizadoEnMeta = const VerificationMeta(
    'actualizadoEn',
  );
  @override
  late final GeneratedColumn<DateTime> actualizadoEn =
      GeneratedColumn<DateTime>(
        'actualizado_en',
        aliasedName,
        false,
        type: DriftSqlType.dateTime,
        requiredDuringInsert: true,
      );
  @override
  List<GeneratedColumn> get $columns => [clave, tipo, contenido, actualizadoEn];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'catalogos';
  @override
  VerificationContext validateIntegrity(
    Insertable<Catalogo> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('clave')) {
      context.handle(
        _claveMeta,
        clave.isAcceptableOrUnknown(data['clave']!, _claveMeta),
      );
    } else if (isInserting) {
      context.missing(_claveMeta);
    }
    if (data.containsKey('tipo')) {
      context.handle(
        _tipoMeta,
        tipo.isAcceptableOrUnknown(data['tipo']!, _tipoMeta),
      );
    }
    if (data.containsKey('contenido')) {
      context.handle(
        _contenidoMeta,
        contenido.isAcceptableOrUnknown(data['contenido']!, _contenidoMeta),
      );
    } else if (isInserting) {
      context.missing(_contenidoMeta);
    }
    if (data.containsKey('actualizado_en')) {
      context.handle(
        _actualizadoEnMeta,
        actualizadoEn.isAcceptableOrUnknown(
          data['actualizado_en']!,
          _actualizadoEnMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_actualizadoEnMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {clave};
  @override
  Catalogo map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Catalogo(
      clave: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}clave'],
      )!,
      tipo: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}tipo'],
      )!,
      contenido: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}contenido'],
      )!,
      actualizadoEn: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}actualizado_en'],
      )!,
    );
  }

  @override
  $CatalogosTable createAlias(String alias) {
    return $CatalogosTable(attachedDatabase, alias);
  }
}

class Catalogo extends DataClass implements Insertable<Catalogo> {
  final String clave;
  final String tipo;
  final String contenido;
  final DateTime actualizadoEn;
  const Catalogo({
    required this.clave,
    required this.tipo,
    required this.contenido,
    required this.actualizadoEn,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['clave'] = Variable<String>(clave);
    map['tipo'] = Variable<String>(tipo);
    map['contenido'] = Variable<String>(contenido);
    map['actualizado_en'] = Variable<DateTime>(actualizadoEn);
    return map;
  }

  CatalogosCompanion toCompanion(bool nullToAbsent) {
    return CatalogosCompanion(
      clave: Value(clave),
      tipo: Value(tipo),
      contenido: Value(contenido),
      actualizadoEn: Value(actualizadoEn),
    );
  }

  factory Catalogo.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Catalogo(
      clave: serializer.fromJson<String>(json['clave']),
      tipo: serializer.fromJson<String>(json['tipo']),
      contenido: serializer.fromJson<String>(json['contenido']),
      actualizadoEn: serializer.fromJson<DateTime>(json['actualizadoEn']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'clave': serializer.toJson<String>(clave),
      'tipo': serializer.toJson<String>(tipo),
      'contenido': serializer.toJson<String>(contenido),
      'actualizadoEn': serializer.toJson<DateTime>(actualizadoEn),
    };
  }

  Catalogo copyWith({
    String? clave,
    String? tipo,
    String? contenido,
    DateTime? actualizadoEn,
  }) => Catalogo(
    clave: clave ?? this.clave,
    tipo: tipo ?? this.tipo,
    contenido: contenido ?? this.contenido,
    actualizadoEn: actualizadoEn ?? this.actualizadoEn,
  );
  Catalogo copyWithCompanion(CatalogosCompanion data) {
    return Catalogo(
      clave: data.clave.present ? data.clave.value : this.clave,
      tipo: data.tipo.present ? data.tipo.value : this.tipo,
      contenido: data.contenido.present ? data.contenido.value : this.contenido,
      actualizadoEn: data.actualizadoEn.present
          ? data.actualizadoEn.value
          : this.actualizadoEn,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Catalogo(')
          ..write('clave: $clave, ')
          ..write('tipo: $tipo, ')
          ..write('contenido: $contenido, ')
          ..write('actualizadoEn: $actualizadoEn')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(clave, tipo, contenido, actualizadoEn);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Catalogo &&
          other.clave == this.clave &&
          other.tipo == this.tipo &&
          other.contenido == this.contenido &&
          other.actualizadoEn == this.actualizadoEn);
}

class CatalogosCompanion extends UpdateCompanion<Catalogo> {
  final Value<String> clave;
  final Value<String> tipo;
  final Value<String> contenido;
  final Value<DateTime> actualizadoEn;
  final Value<int> rowid;
  const CatalogosCompanion({
    this.clave = const Value.absent(),
    this.tipo = const Value.absent(),
    this.contenido = const Value.absent(),
    this.actualizadoEn = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  CatalogosCompanion.insert({
    required String clave,
    this.tipo = const Value.absent(),
    required String contenido,
    required DateTime actualizadoEn,
    this.rowid = const Value.absent(),
  }) : clave = Value(clave),
       contenido = Value(contenido),
       actualizadoEn = Value(actualizadoEn);
  static Insertable<Catalogo> custom({
    Expression<String>? clave,
    Expression<String>? tipo,
    Expression<String>? contenido,
    Expression<DateTime>? actualizadoEn,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (clave != null) 'clave': clave,
      if (tipo != null) 'tipo': tipo,
      if (contenido != null) 'contenido': contenido,
      if (actualizadoEn != null) 'actualizado_en': actualizadoEn,
      if (rowid != null) 'rowid': rowid,
    });
  }

  CatalogosCompanion copyWith({
    Value<String>? clave,
    Value<String>? tipo,
    Value<String>? contenido,
    Value<DateTime>? actualizadoEn,
    Value<int>? rowid,
  }) {
    return CatalogosCompanion(
      clave: clave ?? this.clave,
      tipo: tipo ?? this.tipo,
      contenido: contenido ?? this.contenido,
      actualizadoEn: actualizadoEn ?? this.actualizadoEn,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (clave.present) {
      map['clave'] = Variable<String>(clave.value);
    }
    if (tipo.present) {
      map['tipo'] = Variable<String>(tipo.value);
    }
    if (contenido.present) {
      map['contenido'] = Variable<String>(contenido.value);
    }
    if (actualizadoEn.present) {
      map['actualizado_en'] = Variable<DateTime>(actualizadoEn.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('CatalogosCompanion(')
          ..write('clave: $clave, ')
          ..write('tipo: $tipo, ')
          ..write('contenido: $contenido, ')
          ..write('actualizadoEn: $actualizadoEn, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $OperacionesPendientesTable extends OperacionesPendientes
    with TableInfo<$OperacionesPendientesTable, OperacionesPendiente> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $OperacionesPendientesTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
    'id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _metodoMeta = const VerificationMeta('metodo');
  @override
  late final GeneratedColumn<String> metodo = GeneratedColumn<String>(
    'metodo',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _rutaMeta = const VerificationMeta('ruta');
  @override
  late final GeneratedColumn<String> ruta = GeneratedColumn<String>(
    'ruta',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _cuerpoMeta = const VerificationMeta('cuerpo');
  @override
  late final GeneratedColumn<String> cuerpo = GeneratedColumn<String>(
    'cuerpo',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _creadoEnMeta = const VerificationMeta(
    'creadoEn',
  );
  @override
  late final GeneratedColumn<DateTime> creadoEn = GeneratedColumn<DateTime>(
    'creado_en',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _intentosMeta = const VerificationMeta(
    'intentos',
  );
  @override
  late final GeneratedColumn<int> intentos = GeneratedColumn<int>(
    'intentos',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(0),
  );
  static const VerificationMeta _estadoMeta = const VerificationMeta('estado');
  @override
  late final GeneratedColumn<String> estado = GeneratedColumn<String>(
    'estado',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
    defaultValue: const Constant('PENDIENTE'),
  );
  static const VerificationMeta _proximoIntentoEnMeta = const VerificationMeta(
    'proximoIntentoEn',
  );
  @override
  late final GeneratedColumn<DateTime> proximoIntentoEn =
      GeneratedColumn<DateTime>(
        'proximo_intento_en',
        aliasedName,
        true,
        type: DriftSqlType.dateTime,
        requiredDuringInsert: false,
      );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    metodo,
    ruta,
    cuerpo,
    creadoEn,
    intentos,
    estado,
    proximoIntentoEn,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'operaciones_pendientes';
  @override
  VerificationContext validateIntegrity(
    Insertable<OperacionesPendiente> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('metodo')) {
      context.handle(
        _metodoMeta,
        metodo.isAcceptableOrUnknown(data['metodo']!, _metodoMeta),
      );
    } else if (isInserting) {
      context.missing(_metodoMeta);
    }
    if (data.containsKey('ruta')) {
      context.handle(
        _rutaMeta,
        ruta.isAcceptableOrUnknown(data['ruta']!, _rutaMeta),
      );
    } else if (isInserting) {
      context.missing(_rutaMeta);
    }
    if (data.containsKey('cuerpo')) {
      context.handle(
        _cuerpoMeta,
        cuerpo.isAcceptableOrUnknown(data['cuerpo']!, _cuerpoMeta),
      );
    }
    if (data.containsKey('creado_en')) {
      context.handle(
        _creadoEnMeta,
        creadoEn.isAcceptableOrUnknown(data['creado_en']!, _creadoEnMeta),
      );
    } else if (isInserting) {
      context.missing(_creadoEnMeta);
    }
    if (data.containsKey('intentos')) {
      context.handle(
        _intentosMeta,
        intentos.isAcceptableOrUnknown(data['intentos']!, _intentosMeta),
      );
    }
    if (data.containsKey('estado')) {
      context.handle(
        _estadoMeta,
        estado.isAcceptableOrUnknown(data['estado']!, _estadoMeta),
      );
    }
    if (data.containsKey('proximo_intento_en')) {
      context.handle(
        _proximoIntentoEnMeta,
        proximoIntentoEn.isAcceptableOrUnknown(
          data['proximo_intento_en']!,
          _proximoIntentoEnMeta,
        ),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  OperacionesPendiente map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return OperacionesPendiente(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}id'],
      )!,
      metodo: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}metodo'],
      )!,
      ruta: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}ruta'],
      )!,
      cuerpo: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}cuerpo'],
      ),
      creadoEn: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}creado_en'],
      )!,
      intentos: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}intentos'],
      )!,
      estado: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}estado'],
      )!,
      proximoIntentoEn: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}proximo_intento_en'],
      ),
    );
  }

  @override
  $OperacionesPendientesTable createAlias(String alias) {
    return $OperacionesPendientesTable(attachedDatabase, alias);
  }
}

class OperacionesPendiente extends DataClass
    implements Insertable<OperacionesPendiente> {
  final String id;
  final String metodo;
  final String ruta;
  final String? cuerpo;
  final DateTime creadoEn;
  final int intentos;
  final String estado;
  final DateTime? proximoIntentoEn;
  const OperacionesPendiente({
    required this.id,
    required this.metodo,
    required this.ruta,
    this.cuerpo,
    required this.creadoEn,
    required this.intentos,
    required this.estado,
    this.proximoIntentoEn,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['metodo'] = Variable<String>(metodo);
    map['ruta'] = Variable<String>(ruta);
    if (!nullToAbsent || cuerpo != null) {
      map['cuerpo'] = Variable<String>(cuerpo);
    }
    map['creado_en'] = Variable<DateTime>(creadoEn);
    map['intentos'] = Variable<int>(intentos);
    map['estado'] = Variable<String>(estado);
    if (!nullToAbsent || proximoIntentoEn != null) {
      map['proximo_intento_en'] = Variable<DateTime>(proximoIntentoEn);
    }
    return map;
  }

  OperacionesPendientesCompanion toCompanion(bool nullToAbsent) {
    return OperacionesPendientesCompanion(
      id: Value(id),
      metodo: Value(metodo),
      ruta: Value(ruta),
      cuerpo: cuerpo == null && nullToAbsent
          ? const Value.absent()
          : Value(cuerpo),
      creadoEn: Value(creadoEn),
      intentos: Value(intentos),
      estado: Value(estado),
      proximoIntentoEn: proximoIntentoEn == null && nullToAbsent
          ? const Value.absent()
          : Value(proximoIntentoEn),
    );
  }

  factory OperacionesPendiente.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return OperacionesPendiente(
      id: serializer.fromJson<String>(json['id']),
      metodo: serializer.fromJson<String>(json['metodo']),
      ruta: serializer.fromJson<String>(json['ruta']),
      cuerpo: serializer.fromJson<String?>(json['cuerpo']),
      creadoEn: serializer.fromJson<DateTime>(json['creadoEn']),
      intentos: serializer.fromJson<int>(json['intentos']),
      estado: serializer.fromJson<String>(json['estado']),
      proximoIntentoEn: serializer.fromJson<DateTime?>(
        json['proximoIntentoEn'],
      ),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'metodo': serializer.toJson<String>(metodo),
      'ruta': serializer.toJson<String>(ruta),
      'cuerpo': serializer.toJson<String?>(cuerpo),
      'creadoEn': serializer.toJson<DateTime>(creadoEn),
      'intentos': serializer.toJson<int>(intentos),
      'estado': serializer.toJson<String>(estado),
      'proximoIntentoEn': serializer.toJson<DateTime?>(proximoIntentoEn),
    };
  }

  OperacionesPendiente copyWith({
    String? id,
    String? metodo,
    String? ruta,
    Value<String?> cuerpo = const Value.absent(),
    DateTime? creadoEn,
    int? intentos,
    String? estado,
    Value<DateTime?> proximoIntentoEn = const Value.absent(),
  }) => OperacionesPendiente(
    id: id ?? this.id,
    metodo: metodo ?? this.metodo,
    ruta: ruta ?? this.ruta,
    cuerpo: cuerpo.present ? cuerpo.value : this.cuerpo,
    creadoEn: creadoEn ?? this.creadoEn,
    intentos: intentos ?? this.intentos,
    estado: estado ?? this.estado,
    proximoIntentoEn: proximoIntentoEn.present
        ? proximoIntentoEn.value
        : this.proximoIntentoEn,
  );
  OperacionesPendiente copyWithCompanion(OperacionesPendientesCompanion data) {
    return OperacionesPendiente(
      id: data.id.present ? data.id.value : this.id,
      metodo: data.metodo.present ? data.metodo.value : this.metodo,
      ruta: data.ruta.present ? data.ruta.value : this.ruta,
      cuerpo: data.cuerpo.present ? data.cuerpo.value : this.cuerpo,
      creadoEn: data.creadoEn.present ? data.creadoEn.value : this.creadoEn,
      intentos: data.intentos.present ? data.intentos.value : this.intentos,
      estado: data.estado.present ? data.estado.value : this.estado,
      proximoIntentoEn: data.proximoIntentoEn.present
          ? data.proximoIntentoEn.value
          : this.proximoIntentoEn,
    );
  }

  @override
  String toString() {
    return (StringBuffer('OperacionesPendiente(')
          ..write('id: $id, ')
          ..write('metodo: $metodo, ')
          ..write('ruta: $ruta, ')
          ..write('cuerpo: $cuerpo, ')
          ..write('creadoEn: $creadoEn, ')
          ..write('intentos: $intentos, ')
          ..write('estado: $estado, ')
          ..write('proximoIntentoEn: $proximoIntentoEn')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
    id,
    metodo,
    ruta,
    cuerpo,
    creadoEn,
    intentos,
    estado,
    proximoIntentoEn,
  );
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is OperacionesPendiente &&
          other.id == this.id &&
          other.metodo == this.metodo &&
          other.ruta == this.ruta &&
          other.cuerpo == this.cuerpo &&
          other.creadoEn == this.creadoEn &&
          other.intentos == this.intentos &&
          other.estado == this.estado &&
          other.proximoIntentoEn == this.proximoIntentoEn);
}

class OperacionesPendientesCompanion
    extends UpdateCompanion<OperacionesPendiente> {
  final Value<String> id;
  final Value<String> metodo;
  final Value<String> ruta;
  final Value<String?> cuerpo;
  final Value<DateTime> creadoEn;
  final Value<int> intentos;
  final Value<String> estado;
  final Value<DateTime?> proximoIntentoEn;
  final Value<int> rowid;
  const OperacionesPendientesCompanion({
    this.id = const Value.absent(),
    this.metodo = const Value.absent(),
    this.ruta = const Value.absent(),
    this.cuerpo = const Value.absent(),
    this.creadoEn = const Value.absent(),
    this.intentos = const Value.absent(),
    this.estado = const Value.absent(),
    this.proximoIntentoEn = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  OperacionesPendientesCompanion.insert({
    required String id,
    required String metodo,
    required String ruta,
    this.cuerpo = const Value.absent(),
    required DateTime creadoEn,
    this.intentos = const Value.absent(),
    this.estado = const Value.absent(),
    this.proximoIntentoEn = const Value.absent(),
    this.rowid = const Value.absent(),
  }) : id = Value(id),
       metodo = Value(metodo),
       ruta = Value(ruta),
       creadoEn = Value(creadoEn);
  static Insertable<OperacionesPendiente> custom({
    Expression<String>? id,
    Expression<String>? metodo,
    Expression<String>? ruta,
    Expression<String>? cuerpo,
    Expression<DateTime>? creadoEn,
    Expression<int>? intentos,
    Expression<String>? estado,
    Expression<DateTime>? proximoIntentoEn,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (metodo != null) 'metodo': metodo,
      if (ruta != null) 'ruta': ruta,
      if (cuerpo != null) 'cuerpo': cuerpo,
      if (creadoEn != null) 'creado_en': creadoEn,
      if (intentos != null) 'intentos': intentos,
      if (estado != null) 'estado': estado,
      if (proximoIntentoEn != null) 'proximo_intento_en': proximoIntentoEn,
      if (rowid != null) 'rowid': rowid,
    });
  }

  OperacionesPendientesCompanion copyWith({
    Value<String>? id,
    Value<String>? metodo,
    Value<String>? ruta,
    Value<String?>? cuerpo,
    Value<DateTime>? creadoEn,
    Value<int>? intentos,
    Value<String>? estado,
    Value<DateTime?>? proximoIntentoEn,
    Value<int>? rowid,
  }) {
    return OperacionesPendientesCompanion(
      id: id ?? this.id,
      metodo: metodo ?? this.metodo,
      ruta: ruta ?? this.ruta,
      cuerpo: cuerpo ?? this.cuerpo,
      creadoEn: creadoEn ?? this.creadoEn,
      intentos: intentos ?? this.intentos,
      estado: estado ?? this.estado,
      proximoIntentoEn: proximoIntentoEn ?? this.proximoIntentoEn,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (metodo.present) {
      map['metodo'] = Variable<String>(metodo.value);
    }
    if (ruta.present) {
      map['ruta'] = Variable<String>(ruta.value);
    }
    if (cuerpo.present) {
      map['cuerpo'] = Variable<String>(cuerpo.value);
    }
    if (creadoEn.present) {
      map['creado_en'] = Variable<DateTime>(creadoEn.value);
    }
    if (intentos.present) {
      map['intentos'] = Variable<int>(intentos.value);
    }
    if (estado.present) {
      map['estado'] = Variable<String>(estado.value);
    }
    if (proximoIntentoEn.present) {
      map['proximo_intento_en'] = Variable<DateTime>(proximoIntentoEn.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('OperacionesPendientesCompanion(')
          ..write('id: $id, ')
          ..write('metodo: $metodo, ')
          ..write('ruta: $ruta, ')
          ..write('cuerpo: $cuerpo, ')
          ..write('creadoEn: $creadoEn, ')
          ..write('intentos: $intentos, ')
          ..write('estado: $estado, ')
          ..write('proximoIntentoEn: $proximoIntentoEn, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $MetaLocalTable extends MetaLocal
    with TableInfo<$MetaLocalTable, MetaLocalData> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $MetaLocalTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _claveMeta = const VerificationMeta('clave');
  @override
  late final GeneratedColumn<String> clave = GeneratedColumn<String>(
    'clave',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _valorMeta = const VerificationMeta('valor');
  @override
  late final GeneratedColumn<String> valor = GeneratedColumn<String>(
    'valor',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [clave, valor];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'meta_local';
  @override
  VerificationContext validateIntegrity(
    Insertable<MetaLocalData> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('clave')) {
      context.handle(
        _claveMeta,
        clave.isAcceptableOrUnknown(data['clave']!, _claveMeta),
      );
    } else if (isInserting) {
      context.missing(_claveMeta);
    }
    if (data.containsKey('valor')) {
      context.handle(
        _valorMeta,
        valor.isAcceptableOrUnknown(data['valor']!, _valorMeta),
      );
    } else if (isInserting) {
      context.missing(_valorMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {clave};
  @override
  MetaLocalData map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return MetaLocalData(
      clave: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}clave'],
      )!,
      valor: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}valor'],
      )!,
    );
  }

  @override
  $MetaLocalTable createAlias(String alias) {
    return $MetaLocalTable(attachedDatabase, alias);
  }
}

class MetaLocalData extends DataClass implements Insertable<MetaLocalData> {
  final String clave;
  final String valor;
  const MetaLocalData({required this.clave, required this.valor});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['clave'] = Variable<String>(clave);
    map['valor'] = Variable<String>(valor);
    return map;
  }

  MetaLocalCompanion toCompanion(bool nullToAbsent) {
    return MetaLocalCompanion(clave: Value(clave), valor: Value(valor));
  }

  factory MetaLocalData.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return MetaLocalData(
      clave: serializer.fromJson<String>(json['clave']),
      valor: serializer.fromJson<String>(json['valor']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'clave': serializer.toJson<String>(clave),
      'valor': serializer.toJson<String>(valor),
    };
  }

  MetaLocalData copyWith({String? clave, String? valor}) =>
      MetaLocalData(clave: clave ?? this.clave, valor: valor ?? this.valor);
  MetaLocalData copyWithCompanion(MetaLocalCompanion data) {
    return MetaLocalData(
      clave: data.clave.present ? data.clave.value : this.clave,
      valor: data.valor.present ? data.valor.value : this.valor,
    );
  }

  @override
  String toString() {
    return (StringBuffer('MetaLocalData(')
          ..write('clave: $clave, ')
          ..write('valor: $valor')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(clave, valor);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is MetaLocalData &&
          other.clave == this.clave &&
          other.valor == this.valor);
}

class MetaLocalCompanion extends UpdateCompanion<MetaLocalData> {
  final Value<String> clave;
  final Value<String> valor;
  final Value<int> rowid;
  const MetaLocalCompanion({
    this.clave = const Value.absent(),
    this.valor = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  MetaLocalCompanion.insert({
    required String clave,
    required String valor,
    this.rowid = const Value.absent(),
  }) : clave = Value(clave),
       valor = Value(valor);
  static Insertable<MetaLocalData> custom({
    Expression<String>? clave,
    Expression<String>? valor,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (clave != null) 'clave': clave,
      if (valor != null) 'valor': valor,
      if (rowid != null) 'rowid': rowid,
    });
  }

  MetaLocalCompanion copyWith({
    Value<String>? clave,
    Value<String>? valor,
    Value<int>? rowid,
  }) {
    return MetaLocalCompanion(
      clave: clave ?? this.clave,
      valor: valor ?? this.valor,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (clave.present) {
      map['clave'] = Variable<String>(clave.value);
    }
    if (valor.present) {
      map['valor'] = Variable<String>(valor.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('MetaLocalCompanion(')
          ..write('clave: $clave, ')
          ..write('valor: $valor, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$BaseLocal extends GeneratedDatabase {
  _$BaseLocal(QueryExecutor e) : super(e);
  $BaseLocalManager get managers => $BaseLocalManager(this);
  late final $CatalogosTable catalogos = $CatalogosTable(this);
  late final $OperacionesPendientesTable operacionesPendientes =
      $OperacionesPendientesTable(this);
  late final $MetaLocalTable metaLocal = $MetaLocalTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [
    catalogos,
    operacionesPendientes,
    metaLocal,
  ];
}

typedef $$CatalogosTableCreateCompanionBuilder =
    CatalogosCompanion Function({
      required String clave,
      Value<String> tipo,
      required String contenido,
      required DateTime actualizadoEn,
      Value<int> rowid,
    });
typedef $$CatalogosTableUpdateCompanionBuilder =
    CatalogosCompanion Function({
      Value<String> clave,
      Value<String> tipo,
      Value<String> contenido,
      Value<DateTime> actualizadoEn,
      Value<int> rowid,
    });

class $$CatalogosTableFilterComposer
    extends Composer<_$BaseLocal, $CatalogosTable> {
  $$CatalogosTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get clave => $composableBuilder(
    column: $table.clave,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get tipo => $composableBuilder(
    column: $table.tipo,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get contenido => $composableBuilder(
    column: $table.contenido,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get actualizadoEn => $composableBuilder(
    column: $table.actualizadoEn,
    builder: (column) => ColumnFilters(column),
  );
}

class $$CatalogosTableOrderingComposer
    extends Composer<_$BaseLocal, $CatalogosTable> {
  $$CatalogosTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get clave => $composableBuilder(
    column: $table.clave,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get tipo => $composableBuilder(
    column: $table.tipo,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get contenido => $composableBuilder(
    column: $table.contenido,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get actualizadoEn => $composableBuilder(
    column: $table.actualizadoEn,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$CatalogosTableAnnotationComposer
    extends Composer<_$BaseLocal, $CatalogosTable> {
  $$CatalogosTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get clave =>
      $composableBuilder(column: $table.clave, builder: (column) => column);

  GeneratedColumn<String> get tipo =>
      $composableBuilder(column: $table.tipo, builder: (column) => column);

  GeneratedColumn<String> get contenido =>
      $composableBuilder(column: $table.contenido, builder: (column) => column);

  GeneratedColumn<DateTime> get actualizadoEn => $composableBuilder(
    column: $table.actualizadoEn,
    builder: (column) => column,
  );
}

class $$CatalogosTableTableManager
    extends
        RootTableManager<
          _$BaseLocal,
          $CatalogosTable,
          Catalogo,
          $$CatalogosTableFilterComposer,
          $$CatalogosTableOrderingComposer,
          $$CatalogosTableAnnotationComposer,
          $$CatalogosTableCreateCompanionBuilder,
          $$CatalogosTableUpdateCompanionBuilder,
          (Catalogo, BaseReferences<_$BaseLocal, $CatalogosTable, Catalogo>),
          Catalogo,
          PrefetchHooks Function()
        > {
  $$CatalogosTableTableManager(_$BaseLocal db, $CatalogosTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$CatalogosTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$CatalogosTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$CatalogosTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<String> clave = const Value.absent(),
                Value<String> tipo = const Value.absent(),
                Value<String> contenido = const Value.absent(),
                Value<DateTime> actualizadoEn = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => CatalogosCompanion(
                clave: clave,
                tipo: tipo,
                contenido: contenido,
                actualizadoEn: actualizadoEn,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required String clave,
                Value<String> tipo = const Value.absent(),
                required String contenido,
                required DateTime actualizadoEn,
                Value<int> rowid = const Value.absent(),
              }) => CatalogosCompanion.insert(
                clave: clave,
                tipo: tipo,
                contenido: contenido,
                actualizadoEn: actualizadoEn,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$CatalogosTableProcessedTableManager =
    ProcessedTableManager<
      _$BaseLocal,
      $CatalogosTable,
      Catalogo,
      $$CatalogosTableFilterComposer,
      $$CatalogosTableOrderingComposer,
      $$CatalogosTableAnnotationComposer,
      $$CatalogosTableCreateCompanionBuilder,
      $$CatalogosTableUpdateCompanionBuilder,
      (Catalogo, BaseReferences<_$BaseLocal, $CatalogosTable, Catalogo>),
      Catalogo,
      PrefetchHooks Function()
    >;
typedef $$OperacionesPendientesTableCreateCompanionBuilder =
    OperacionesPendientesCompanion Function({
      required String id,
      required String metodo,
      required String ruta,
      Value<String?> cuerpo,
      required DateTime creadoEn,
      Value<int> intentos,
      Value<String> estado,
      Value<DateTime?> proximoIntentoEn,
      Value<int> rowid,
    });
typedef $$OperacionesPendientesTableUpdateCompanionBuilder =
    OperacionesPendientesCompanion Function({
      Value<String> id,
      Value<String> metodo,
      Value<String> ruta,
      Value<String?> cuerpo,
      Value<DateTime> creadoEn,
      Value<int> intentos,
      Value<String> estado,
      Value<DateTime?> proximoIntentoEn,
      Value<int> rowid,
    });

class $$OperacionesPendientesTableFilterComposer
    extends Composer<_$BaseLocal, $OperacionesPendientesTable> {
  $$OperacionesPendientesTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get metodo => $composableBuilder(
    column: $table.metodo,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get ruta => $composableBuilder(
    column: $table.ruta,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get cuerpo => $composableBuilder(
    column: $table.cuerpo,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get creadoEn => $composableBuilder(
    column: $table.creadoEn,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get intentos => $composableBuilder(
    column: $table.intentos,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get estado => $composableBuilder(
    column: $table.estado,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get proximoIntentoEn => $composableBuilder(
    column: $table.proximoIntentoEn,
    builder: (column) => ColumnFilters(column),
  );
}

class $$OperacionesPendientesTableOrderingComposer
    extends Composer<_$BaseLocal, $OperacionesPendientesTable> {
  $$OperacionesPendientesTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get metodo => $composableBuilder(
    column: $table.metodo,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get ruta => $composableBuilder(
    column: $table.ruta,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get cuerpo => $composableBuilder(
    column: $table.cuerpo,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get creadoEn => $composableBuilder(
    column: $table.creadoEn,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get intentos => $composableBuilder(
    column: $table.intentos,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get estado => $composableBuilder(
    column: $table.estado,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get proximoIntentoEn => $composableBuilder(
    column: $table.proximoIntentoEn,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$OperacionesPendientesTableAnnotationComposer
    extends Composer<_$BaseLocal, $OperacionesPendientesTable> {
  $$OperacionesPendientesTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get metodo =>
      $composableBuilder(column: $table.metodo, builder: (column) => column);

  GeneratedColumn<String> get ruta =>
      $composableBuilder(column: $table.ruta, builder: (column) => column);

  GeneratedColumn<String> get cuerpo =>
      $composableBuilder(column: $table.cuerpo, builder: (column) => column);

  GeneratedColumn<DateTime> get creadoEn =>
      $composableBuilder(column: $table.creadoEn, builder: (column) => column);

  GeneratedColumn<int> get intentos =>
      $composableBuilder(column: $table.intentos, builder: (column) => column);

  GeneratedColumn<String> get estado =>
      $composableBuilder(column: $table.estado, builder: (column) => column);

  GeneratedColumn<DateTime> get proximoIntentoEn => $composableBuilder(
    column: $table.proximoIntentoEn,
    builder: (column) => column,
  );
}

class $$OperacionesPendientesTableTableManager
    extends
        RootTableManager<
          _$BaseLocal,
          $OperacionesPendientesTable,
          OperacionesPendiente,
          $$OperacionesPendientesTableFilterComposer,
          $$OperacionesPendientesTableOrderingComposer,
          $$OperacionesPendientesTableAnnotationComposer,
          $$OperacionesPendientesTableCreateCompanionBuilder,
          $$OperacionesPendientesTableUpdateCompanionBuilder,
          (
            OperacionesPendiente,
            BaseReferences<
              _$BaseLocal,
              $OperacionesPendientesTable,
              OperacionesPendiente
            >,
          ),
          OperacionesPendiente,
          PrefetchHooks Function()
        > {
  $$OperacionesPendientesTableTableManager(
    _$BaseLocal db,
    $OperacionesPendientesTable table,
  ) : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$OperacionesPendientesTableFilterComposer(
                $db: db,
                $table: table,
              ),
          createOrderingComposer: () =>
              $$OperacionesPendientesTableOrderingComposer(
                $db: db,
                $table: table,
              ),
          createComputedFieldComposer: () =>
              $$OperacionesPendientesTableAnnotationComposer(
                $db: db,
                $table: table,
              ),
          updateCompanionCallback:
              ({
                Value<String> id = const Value.absent(),
                Value<String> metodo = const Value.absent(),
                Value<String> ruta = const Value.absent(),
                Value<String?> cuerpo = const Value.absent(),
                Value<DateTime> creadoEn = const Value.absent(),
                Value<int> intentos = const Value.absent(),
                Value<String> estado = const Value.absent(),
                Value<DateTime?> proximoIntentoEn = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => OperacionesPendientesCompanion(
                id: id,
                metodo: metodo,
                ruta: ruta,
                cuerpo: cuerpo,
                creadoEn: creadoEn,
                intentos: intentos,
                estado: estado,
                proximoIntentoEn: proximoIntentoEn,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required String id,
                required String metodo,
                required String ruta,
                Value<String?> cuerpo = const Value.absent(),
                required DateTime creadoEn,
                Value<int> intentos = const Value.absent(),
                Value<String> estado = const Value.absent(),
                Value<DateTime?> proximoIntentoEn = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => OperacionesPendientesCompanion.insert(
                id: id,
                metodo: metodo,
                ruta: ruta,
                cuerpo: cuerpo,
                creadoEn: creadoEn,
                intentos: intentos,
                estado: estado,
                proximoIntentoEn: proximoIntentoEn,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$OperacionesPendientesTableProcessedTableManager =
    ProcessedTableManager<
      _$BaseLocal,
      $OperacionesPendientesTable,
      OperacionesPendiente,
      $$OperacionesPendientesTableFilterComposer,
      $$OperacionesPendientesTableOrderingComposer,
      $$OperacionesPendientesTableAnnotationComposer,
      $$OperacionesPendientesTableCreateCompanionBuilder,
      $$OperacionesPendientesTableUpdateCompanionBuilder,
      (
        OperacionesPendiente,
        BaseReferences<
          _$BaseLocal,
          $OperacionesPendientesTable,
          OperacionesPendiente
        >,
      ),
      OperacionesPendiente,
      PrefetchHooks Function()
    >;
typedef $$MetaLocalTableCreateCompanionBuilder =
    MetaLocalCompanion Function({
      required String clave,
      required String valor,
      Value<int> rowid,
    });
typedef $$MetaLocalTableUpdateCompanionBuilder =
    MetaLocalCompanion Function({
      Value<String> clave,
      Value<String> valor,
      Value<int> rowid,
    });

class $$MetaLocalTableFilterComposer
    extends Composer<_$BaseLocal, $MetaLocalTable> {
  $$MetaLocalTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get clave => $composableBuilder(
    column: $table.clave,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get valor => $composableBuilder(
    column: $table.valor,
    builder: (column) => ColumnFilters(column),
  );
}

class $$MetaLocalTableOrderingComposer
    extends Composer<_$BaseLocal, $MetaLocalTable> {
  $$MetaLocalTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get clave => $composableBuilder(
    column: $table.clave,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get valor => $composableBuilder(
    column: $table.valor,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$MetaLocalTableAnnotationComposer
    extends Composer<_$BaseLocal, $MetaLocalTable> {
  $$MetaLocalTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get clave =>
      $composableBuilder(column: $table.clave, builder: (column) => column);

  GeneratedColumn<String> get valor =>
      $composableBuilder(column: $table.valor, builder: (column) => column);
}

class $$MetaLocalTableTableManager
    extends
        RootTableManager<
          _$BaseLocal,
          $MetaLocalTable,
          MetaLocalData,
          $$MetaLocalTableFilterComposer,
          $$MetaLocalTableOrderingComposer,
          $$MetaLocalTableAnnotationComposer,
          $$MetaLocalTableCreateCompanionBuilder,
          $$MetaLocalTableUpdateCompanionBuilder,
          (
            MetaLocalData,
            BaseReferences<_$BaseLocal, $MetaLocalTable, MetaLocalData>,
          ),
          MetaLocalData,
          PrefetchHooks Function()
        > {
  $$MetaLocalTableTableManager(_$BaseLocal db, $MetaLocalTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$MetaLocalTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$MetaLocalTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$MetaLocalTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<String> clave = const Value.absent(),
                Value<String> valor = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) =>
                  MetaLocalCompanion(clave: clave, valor: valor, rowid: rowid),
          createCompanionCallback:
              ({
                required String clave,
                required String valor,
                Value<int> rowid = const Value.absent(),
              }) => MetaLocalCompanion.insert(
                clave: clave,
                valor: valor,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$MetaLocalTableProcessedTableManager =
    ProcessedTableManager<
      _$BaseLocal,
      $MetaLocalTable,
      MetaLocalData,
      $$MetaLocalTableFilterComposer,
      $$MetaLocalTableOrderingComposer,
      $$MetaLocalTableAnnotationComposer,
      $$MetaLocalTableCreateCompanionBuilder,
      $$MetaLocalTableUpdateCompanionBuilder,
      (
        MetaLocalData,
        BaseReferences<_$BaseLocal, $MetaLocalTable, MetaLocalData>,
      ),
      MetaLocalData,
      PrefetchHooks Function()
    >;

class $BaseLocalManager {
  final _$BaseLocal _db;
  $BaseLocalManager(this._db);
  $$CatalogosTableTableManager get catalogos =>
      $$CatalogosTableTableManager(_db, _db.catalogos);
  $$OperacionesPendientesTableTableManager get operacionesPendientes =>
      $$OperacionesPendientesTableTableManager(_db, _db.operacionesPendientes);
  $$MetaLocalTableTableManager get metaLocal =>
      $$MetaLocalTableTableManager(_db, _db.metaLocal);
}
