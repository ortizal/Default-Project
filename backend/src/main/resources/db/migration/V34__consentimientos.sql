-- =====================================================================
-- V34: Consentimientos informados — Fase E
-- ---------------------------------------------------------------------
-- Fase E del plan de ampliación (§4.13): catálogo de plantillas
-- (tratamiento general, extracción, endodoncia, implante, cirugía,
-- ortodoncia, imágenes y blanqueamiento) y consentimientos por paciente
-- con snapshot inmutable del texto, firma del paciente/tutor, hash
-- SHA-256 e informe PDF. El consentimiento sólo se edita mientras está
-- en BORRADOR: al firmarse queda congelado.
-- =====================================================================

CREATE TABLE consentimiento_plantillas (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL REFERENCES tenants (id),
    codigo      VARCHAR(40) NOT NULL,
    nombre      VARCHAR(150) NOT NULL,
    categoria   VARCHAR(60),
    contenido   TEXT NOT NULL,
    version     INT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_consentimiento_plantilla UNIQUE (tenant_id, codigo)
);

CREATE TABLE consentimientos (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL REFERENCES tenants (id),
    paciente_id       BIGINT NOT NULL REFERENCES pacientes (id) ON DELETE CASCADE,
    plantilla_codigo  VARCHAR(40) NOT NULL,
    plantilla_version INT NOT NULL,
    titulo            VARCHAR(150) NOT NULL,
    categoria         VARCHAR(60),
    contenido         TEXT NOT NULL,
    fecha             DATE NOT NULL DEFAULT CURRENT_DATE,
    tutor_nombre      VARCHAR(150),
    odontologo_id     BIGINT REFERENCES odontologos (id),
    estado            VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                      CHECK (estado IN ('BORRADOR', 'FIRMADO')),
    firmante          VARCHAR(150),
    firma_imagen      TEXT,
    firma_hash        VARCHAR(64),
    firmado_en        TIMESTAMPTZ,
    observaciones     VARCHAR(500),
    created_by        BIGINT REFERENCES usuarios (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consentimientos_paciente ON consentimientos (paciente_id, fecha DESC);

-- ---------------------------------------------------------------------
-- Plantillas sembradas para cada tenant existente
-- ---------------------------------------------------------------------
INSERT INTO consentimiento_plantillas (tenant_id, codigo, nombre, categoria, contenido)
SELECT t.id, x.codigo, x.nombre, x.categoria, x.contenido
FROM tenants t
CROSS JOIN (VALUES
    ('TRATAMIENTO_GENERAL', 'Consentimiento para tratamiento dental', 'Tratamientos',
'El paciente declara haber sido informado por el profesional tratante sobre el diagnóstico, las
alternativas de tratamiento, los riesgos esperables, los beneficios, los costos aproximados y las
alternativas de no tratar. Se compromete a comunicar cualquier antecedente médico, alergia o
medicación en curso, y a seguir las indicaciones y los controles indicados.

Autoriza la realización del tratamiento dental descrito, así como la administración de anestesia
local y las medidas de profilaxis que el profesional considere necesarias. Comprende que ningún
tratamiento garantiza la ausencia total de complicaciones y que podrían requerirse sesiones
adicionales.'),

    ('EXTRACCION', 'Consentimiento para extracción dental', 'Cirugía',
'El paciente autoriza la extracción de la pieza o piezas dentarias indicadas por el profesional,
incluido el curetaje alveolar y la sutura cuando sea necesario.

Se le ha informado de los riesgos habituales: dolor, sangrado, hinchazón, infección, limitación de
la apertura bucal, lesión de nervios cercanos (entumecimiento de labio, lengua o mentón), de
senos maxilares, fractura de la raíz o del hueso, y la posibilidad de alveolitis seca. También se
ha explicado la necesidad de conservar el coágulo, evitar el tabaco y las enjuagues durante las
primeras horas, así como las opciones de sustitución de la pieza.'),

    ('ENDODONCIA', 'Consentimiento para endodoncia', 'Tratamientos',
'El paciente autoriza el tratamiento de conductos (endodoncia) de la pieza indicada, con el fin de
conservarla en boca. Se le ha informado de que el tratamiento puede requerir más de una sesión y
de sus riesgos: dolor postoperatorio, reactivación de la infección, fractura o perforación de la
raíz, separación de instrumentos, reabsorción y, en última instancia, la extracción de la pieza.

Comprende que la pieza endodonciada se debilita y que habitualmente requiere una restauración o
corona de protección, y que el éxito depende de factores como la anatomía de la raíz y el estado
previo de la infección.'),

    ('IMPLANTE', 'Consentimiento para implante dental', 'Cirugía',
'El paciente autoriza la colocación de implante(s) oral(es) descrito(s) por el profesional, que
puede incluir injerto óseo y técnicas de regeneración cuando el hueso disponible lo exija.

Se le ha informado de los riesgos: dolor, sangrado, hinchazón, infección, lesión de nervios o del
seno maxilar, fracaso del implante por falta de osteointegración, pérdida ósea, periimplantitis y
la necesidad de cirugías adicionales o recambio del implante. Acepta cuidar la higiene, acudir a los
controles y evitar el tabaco, que aumenta el riesgo de complicaciones.'),

    ('CIRUGIA_ORAL', 'Consentimiento para cirugía oral', 'Cirugía',
'El paciente autoriza la intervención de cirugía oral descrita (biopsia, frenectomía, resección de
lesión, apicectomía u otra) bajo anestesia local y/o sedación indicada.

Se le ha informado de los riesgos: dolor, sangrado, hinchazón, infección, limitación de la
apertura bucal, dehiscencia de la herida, alteraciones de la sensibilidad, lesión de estructuras
vecinas (nervios, dientes, senos) y la posibilidad de necesitar tratamiento posterior. El paciente
consiente la toma de registros fotográficos y la conservación de la pieza de tejido para estudio
histopatológico.'),

    ('ORTODONCIA', 'Consentimiento para ortodoncia', 'Ortodoncia',
'El paciente (o su tutor) autoriza el tratamiento de ortodoncia descrito, con la duración estimada
y el número de revisiones indicados por el profesional.

Se le ha informado de que el resultado depende de la colaboración (uso de elásticos y aparatos
removibles, higiene y controles), de que pueden producirse recesiones gingivales, caries,
descalcificación, dolor, movilidad temporal de las piezas y que existe un riesgo de recidiva
(reaproximación) tras la finalización, por lo que será necesaria la retención indicada.'),

    ('IMAGENES', 'Autorización para imágenes diagnósticas', 'Diagnóstico',
'El paciente autoriza la obtención de imágenes diagnósticas (radiografías intraorales,
panorámica, tomografía o fotografías clínicas) indicadas por el profesional para el establecimiento
del diagnóstico y el seguimiento del tratamiento.

Se le ha informado de que las radiografías implican una exposición a radiación ionizante, muy baja
y justificada clínicamente, y de que las fotografías forman parte de su historia clínica. Acepta
que los registros se conserven en la historia clínica y se usen exclusivamente con fines de
diagnóstico, tratamiento y control sanitario.'),

    ('BLANQUEAMIENTO', 'Consentimiento para blanqueamiento dental', 'Estética',
'El paciente autoriza el blanqueamiento dental (en consultorio y/o domiciliario) de la pieza o
piezas indicadas.

Se le ha informado de los riesgos: sensibilidad transitoria, irritación de encías, alteraciones del
esmalte y variación del resultado final, que depende del color inicial y de la causa de la
coloración. Comprende que el resultado no es definitivo ni uniforme, que puede requerir retoques y
que no blanquea empastes, coronas ni prótesis, los cuales podrían necesitarse cambiar después.'
    )) AS x(codigo, nombre, categoria, contenido);

-- ---------------------------------------------------------------------
-- CONSENTIMIENTOS_READ para RECEPCION (Fase E, §4.13)
-- ---------------------------------------------------------------------
-- La recepción puede consultar e imprimir el consentimiento en el
-- mostrador (igual que historia clínica, odontograma, planes, periodoncia
-- y recetas), pero no lo redacta ni lo firma (sin CONSENTIMIENTOS_WRITE).
INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
JOIN permisos p ON p.codigo = 'CONSENTIMIENTOS_READ'
WHERE r.codigo = 'RECEPCION'
ON CONFLICT DO NOTHING;
