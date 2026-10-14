-- =====================================================================
-- V37: Cuenta del paciente — Fase F (§3.3)
-- ---------------------------------------------------------------------
-- Capa financiera previa al comprobante electrónico: cargos clínicos y
-- manuales, descuentos, anticipos, abonos y devoluciones. El saldo NO es
-- una columna editable: se recalcula siempre a partir de los movimientos
-- del libro mayor (cuenta_detalles), donde cada tipo tiene signo propio.
--
--   saldo = Σ CARGO − Σ DESCUENTO − Σ ABONO − Σ ANTICIPO + Σ DEVOLUCION
--
-- Estados de la cuenta: ABIERTA · PARCIAL · PAGADA · CERRADA · ANULADA.
-- Los tres primeros se derivan del saldo y de los créditos acumulados;
-- CERRADA y ANULADA sólo se fijan a mano.
--
-- Nota de diseño (plan §3.14 lista las tablas como "sugeridas"): no se
-- crea una tabla `pagos` aparte. El abono/anticipo YA es un movimiento
-- del libro con su forma de pago, por lo que `GET /facturacion/pagos`
-- sólo consulta esos movimientos: una sola fuente de verdad para el
-- dinero que entra y cero duplicidad de saldos.
-- =====================================================================

CREATE TABLE cuentas_paciente (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL REFERENCES tenants (id),
    paciente_id       BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    estado            VARCHAR(20) NOT NULL DEFAULT 'ABIERTA'
                      CHECK (estado IN ('ABIERTA', 'PARCIAL', 'PAGADA',
                                        'CERRADA', 'ANULADA')),
    saldo             NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_cargos      NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_descuentos  NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_abonos      NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_by        BIGINT REFERENCES usuarios (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cuenta_paciente UNIQUE (tenant_id, paciente_id)
);

CREATE INDEX idx_cuentas_paciente_estado ON cuentas_paciente (estado);
CREATE INDEX idx_cuentas_paciente_saldo ON cuentas_paciente (saldo);

-- Libro mayor de la cuenta: un registro por cada cargo, descuento,
-- abono, anticipo o devolución. `total` siempre es >= 0; el signo lo
-- define `tipo`. `origen` permite que el mismo procedimiento facturable
-- sólo genere un cargo (unicidad de referencia) y que la reversión del
-- procedimiento anule exactamente ese cargo.
CREATE TABLE cuenta_detalles (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL REFERENCES tenants (id),
    cuenta_id        BIGINT NOT NULL REFERENCES cuentas_paciente (id) ON DELETE CASCADE,
    tipo             VARCHAR(20) NOT NULL
                     CHECK (tipo IN ('CARGO', 'DESCUENTO', 'ABONO',
                                     'ANTICIPO', 'DEVOLUCION')),
    origen           VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
                     CHECK (origen IN ('MANUAL', 'PROCEDIMIENTO', 'CAJA')),
    referencia_id    BIGINT,
    -- Dimensión de los reportes (§3.12/§3.13): producción por odontólogo
    -- e ingresos por servicio. Sólo los cargos del plan los traen.
    odontologo_id    BIGINT REFERENCES odontologos (id),
    servicio_id      BIGINT REFERENCES servicios (id),
    concepto         VARCHAR(255) NOT NULL,
    diente           INT CHECK (diente IS NULL OR (diente >= 11 AND diente <= 48)),
    superficie       VARCHAR(20),
    cantidad         INT NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    precio_unitario  NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento_pct    NUMERIC(5,2) NOT NULL DEFAULT 0
                     CHECK (descuento_pct >= 0 AND descuento_pct <= 100),
    total            NUMERIC(14,2) NOT NULL CHECK (total >= 0),
    forma_pago       VARCHAR(20)
                     CHECK (forma_pago IS NULL OR forma_pago IN
                            ('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'OTRO')),
    fecha            DATE NOT NULL DEFAULT CURRENT_DATE,
    observaciones    VARCHAR(500),
    anulado          BOOLEAN NOT NULL DEFAULT FALSE,
    anulado_motivo   VARCHAR(255),
    created_by       BIGINT REFERENCES usuarios (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_cuenta_detalle_forma
        CHECK (tipo NOT IN ('ABONO', 'ANTICIPO', 'DEVOLUCION')
               OR forma_pago IS NOT NULL)
);

-- Idempotencia: un procedimiento completado genera UN sólo cargo aunque
-- el estado se cambie varias veces (los movimientos manuales usan NULL
-- y Postgres no colisiona con ellos).
CREATE UNIQUE INDEX uq_cuenta_detalle_referencia
    ON cuenta_detalles (cuenta_id, origen, referencia_id)
    WHERE referencia_id IS NOT NULL;

CREATE INDEX idx_cuenta_detalles_cuenta
    ON cuenta_detalles (cuenta_id, fecha, id);
CREATE INDEX idx_cuenta_detalles_fecha ON cuenta_detalles (fecha);
CREATE INDEX idx_cuenta_detalles_tipo ON cuenta_detalles (tipo, anulado);
CREATE INDEX idx_cuenta_detalles_tenant ON cuenta_detalles (tenant_id);
CREATE INDEX idx_cuenta_detalles_odontologo ON cuenta_detalles (odontologo_id);
CREATE INDEX idx_cuenta_detalles_servicio ON cuenta_detalles (servicio_id);
