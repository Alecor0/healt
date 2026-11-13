"""Medicamentos, recordatorios, dosis, citas y signos vitales. Solo agrega; no borra datos.

En Neon estas tablas ya existen (vacías): solo se les agregan las columnas que la
app necesita. En una base nueva o de pruebas se crean con el mismo esquema.
"""
from alembic import op
import sqlalchemy as sa

revision = "0003_modulos_de_salud"
down_revision = "0002_sesiones_y_perfil"
branch_labels = None
depends_on = None

MOODS = "'triste', 'normal', 'feliz', 'emocionado'"


def uuid_pk():
    return sa.Column("id", sa.Uuid(), primary_key=True, server_default=sa.text("uuid_generate_v4()"))


def fk(column, target, ondelete="CASCADE"):
    return sa.Column(column, sa.Uuid(), sa.ForeignKey(target, ondelete=ondelete), nullable=False)


def create_missing_tables():
    existing = set(sa.inspect(op.get_bind()).get_table_names())
    if "medicamentos" not in existing:
        op.create_table(
            "medicamentos",
            uuid_pk(),
            sa.Column("nombre", sa.String(255), nullable=False),
            sa.Column("descripcion", sa.Text()),
            sa.Column("compuesto_activo", sa.String(255)),
            sa.UniqueConstraint("nombre", "compuesto_activo", name="medicamentos_nombre_compuesto_activo_key"),
        )
    if "usuario_medicamento" not in existing:
        op.create_table(
            "usuario_medicamento",
            uuid_pk(),
            fk("usuario_id", "usuarios.id"),
            fk("medicamento_id", "medicamentos.id", "RESTRICT"),
            sa.Column("dosis", sa.String(100)),
            sa.Column("frecuencia", sa.String(100)),
            sa.Column("fecha_inicio", sa.Date()),
            sa.Column("fecha_fin", sa.Date()),
            sa.Column("activo", sa.Boolean(), server_default=sa.true(), nullable=False),
            sa.CheckConstraint("fecha_fin IS NULL OR fecha_fin >= fecha_inicio", name="usuario_medicamento_check"),
        )
        op.create_index("idx_usuario_medicamento_usuario", "usuario_medicamento", ["usuario_id"])
    if "recordatorios" not in existing:
        op.create_table(
            "recordatorios",
            uuid_pk(),
            fk("usuario_medicamento_id", "usuario_medicamento.id"),
            sa.Column("hora", sa.Time(), nullable=False),
            sa.Column("dias_semana", sa.String(20)),
            sa.Column("activo", sa.Boolean(), server_default=sa.true(), nullable=False),
        )
        op.create_index("idx_recordatorios_um", "recordatorios", ["usuario_medicamento_id"])
    if "dosis_tomada" not in existing:
        op.create_table(
            "dosis_tomada",
            uuid_pk(),
            fk("recordatorio_id", "recordatorios.id"),
            sa.Column("fecha_programada", sa.DateTime(timezone=True), nullable=False),
            sa.Column("fecha_confirmacion", sa.DateTime(timezone=True)),
            sa.Column("estado", sa.String(50), server_default="pendiente", nullable=False),
            sa.CheckConstraint(
                "fecha_confirmacion IS NULL OR fecha_confirmacion >= fecha_programada",
                name="dosis_tomada_check",
            ),
            sa.CheckConstraint(
                "estado IN ('pendiente', 'tomado', 'omitido', 'pospuesto')", name="dosis_tomada_estado_check"
            ),
        )
    if "citas" not in existing:
        op.create_table(
            "citas",
            uuid_pk(),
            fk("paciente_id", "usuarios.id"),
            sa.Column("especialista", sa.String(255)),
            sa.Column("lugar", sa.String(255)),
            sa.Column("fecha_hora", sa.DateTime(timezone=True), nullable=False),
            sa.Column("motivo", sa.Text()),
            sa.Column("estado", sa.String(50), server_default="pendiente", nullable=False),
            sa.CheckConstraint(
                "estado IN ('pendiente', 'confirmada', 'cancelada', 'completada')", name="citas_estado_check"
            ),
        )
        op.create_index("idx_citas_paciente_fecha", "citas", ["paciente_id", "fecha_hora"])
    if "signos_vitales" not in existing:
        op.create_table(
            "signos_vitales",
            uuid_pk(),
            fk("paciente_id", "usuarios.id"),
            sa.Column("presion_sistolica", sa.Integer()),
            sa.Column("presion_diastolica", sa.Integer()),
            sa.Column("frecuencia_cardiaca", sa.Integer()),
            sa.Column("oxigenacion", sa.Integer()),
            sa.Column("glucosa", sa.Numeric(5, 2)),
            sa.Column("temperatura", sa.Numeric(4, 2)),
            sa.Column("fecha_registro", sa.DateTime(timezone=True), server_default=sa.func.current_timestamp()),
            sa.CheckConstraint(
                "presion_sistolica >= 40 AND presion_sistolica <= 250",
                name="signos_vitales_presion_sistolica_check",
            ),
            sa.CheckConstraint(
                "presion_diastolica >= 30 AND presion_diastolica <= 150",
                name="signos_vitales_presion_diastolica_check",
            ),
            sa.CheckConstraint("frecuencia_cardiaca > 0", name="signos_vitales_frecuencia_cardiaca_check"),
            sa.CheckConstraint(
                "oxigenacion >= 0 AND oxigenacion <= 100", name="signos_vitales_oxigenacion_check"
            ),
            sa.CheckConstraint(
                "temperatura >= 30 AND temperatura <= 45", name="signos_vitales_temperatura_check"
            ),
        )
        op.create_index("idx_signos_paciente_fecha", "signos_vitales", ["paciente_id", "fecha_registro"])


def upgrade():
    create_missing_tables()

    # RF-02: vía de administración y propósito; borrado lógico (caso límite 4.5).
    op.add_column("usuario_medicamento", sa.Column("via_administracion", sa.String(50)))
    op.add_column("usuario_medicamento", sa.Column("proposito", sa.String(255)))
    op.add_column("usuario_medicamento", sa.Column("eliminado_en", sa.DateTime(timezone=True)))

    # RF-03: minutos pospuestos y una sola dosis por recordatorio y horario.
    op.add_column(
        "dosis_tomada", sa.Column("pospuesto_minutos", sa.Integer(), server_default="0", nullable=False)
    )
    op.create_unique_constraint(
        "uq_dosis_tomada_recordatorio_fecha", "dosis_tomada", ["recordatorio_id", "fecha_programada"]
    )

    # RF-04: especialidad del especialista.
    op.add_column("citas", sa.Column("especialidad", sa.String(100)))

    # RF-05: peso, escala de dolor, estado de ánimo y notas junto a los signos vitales.
    op.add_column("signos_vitales", sa.Column("peso", sa.Numeric(5, 2)))
    op.add_column("signos_vitales", sa.Column("dolor", sa.Integer()))
    op.add_column("signos_vitales", sa.Column("estado_animo", sa.String(20)))
    op.add_column("signos_vitales", sa.Column("notas", sa.Text()))
    op.create_check_constraint("ck_signos_vitales_peso", "signos_vitales", "peso > 0 AND peso <> 'NaN'::numeric")
    op.create_check_constraint("ck_signos_vitales_dolor", "signos_vitales", "dolor >= 1 AND dolor <= 10")
    op.create_check_constraint(
        "ck_signos_vitales_estado_animo", "signos_vitales", f"estado_animo IN ({MOODS})"
    )


def downgrade():
    # Quita solo lo agregado; las tablas quedan porque en Neon ya existían.
    op.drop_constraint("ck_signos_vitales_estado_animo", "signos_vitales", type_="check")
    op.drop_constraint("ck_signos_vitales_dolor", "signos_vitales", type_="check")
    op.drop_constraint("ck_signos_vitales_peso", "signos_vitales", type_="check")
    for column in ("notas", "estado_animo", "dolor", "peso"):
        op.drop_column("signos_vitales", column)
    op.drop_column("citas", "especialidad")
    op.drop_constraint("uq_dosis_tomada_recordatorio_fecha", "dosis_tomada", type_="unique")
    op.drop_column("dosis_tomada", "pospuesto_minutos")
    for column in ("eliminado_en", "proposito", "via_administracion"):
        op.drop_column("usuario_medicamento", column)
