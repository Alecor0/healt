"""Agrega lo que la API necesita y no existía en Neon. Solo agrega; no borra datos.

- Sesiones de renovación (refresh tokens) y desafíos de Google.
- Peso, altura y contacto de emergencia en perfiles (RF-01), opcionales.
"""
from alembic import op
import sqlalchemy as sa

revision = "0002_sesiones_y_perfil"
down_revision = "0001_esquema_neon"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "sesiones_refresh",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("usuario_id", sa.Uuid(), sa.ForeignKey("usuarios.id", ondelete="CASCADE"), nullable=False),
        sa.Column("vence_en", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_sesiones_refresh_usuario_id", "sesiones_refresh", ["usuario_id"])
    op.create_index("ix_sesiones_refresh_vence_en", "sesiones_refresh", ["vence_en"])
    op.create_table(
        "desafios_google",
        sa.Column("nonce", sa.Uuid(), primary_key=True),
        sa.Column("vence_en", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_desafios_google_vence_en", "desafios_google", ["vence_en"])

    op.add_column("perfiles", sa.Column("peso", sa.Numeric(5, 2)))
    op.add_column("perfiles", sa.Column("altura", sa.Integer()))
    op.add_column("perfiles", sa.Column("contacto_emergencia", sa.String(100)))
    op.create_check_constraint("ck_perfiles_peso", "perfiles", "peso > 0 AND peso <> 'NaN'::numeric")
    op.create_check_constraint("ck_perfiles_altura", "perfiles", "altura > 0")


def downgrade():
    op.drop_constraint("ck_perfiles_altura", "perfiles", type_="check")
    op.drop_constraint("ck_perfiles_peso", "perfiles", type_="check")
    op.drop_column("perfiles", "contacto_emergencia")
    op.drop_column("perfiles", "altura")
    op.drop_column("perfiles", "peso")
    op.drop_table("desafios_google")
    op.drop_table("sesiones_refresh")
