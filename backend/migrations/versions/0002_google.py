"""Permite iniciar sesión con Google sin modificar las cuentas existentes."""

import sqlalchemy as sa
from alembic import op

revision = "0002_google"
down_revision = "0001_usuarios"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("usuarios", sa.Column("google_sub", sa.String(255)))
    op.alter_column("usuarios", "password_hash", existing_type=sa.String(255), nullable=True)
    op.create_unique_constraint("uq_usuarios_google_sub", "usuarios", ["google_sub"])
    op.create_check_constraint(
        "ck_usuarios_credencial", "usuarios",
        "password_hash IS NOT NULL OR google_sub IS NOT NULL",
    )
    op.create_table(
        "desafios_google",
        sa.Column("nonce", sa.Uuid(), primary_key=True),
        sa.Column("vence_en", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_desafios_google_vence_en", "desafios_google", ["vence_en"])


def downgrade():
    op.alter_column("usuarios", "password_hash", existing_type=sa.String(255), nullable=False)
    op.drop_table("desafios_google")
    op.drop_constraint("ck_usuarios_credencial", "usuarios", type_="check")
    op.drop_constraint("uq_usuarios_google_sub", "usuarios", type_="unique")
    op.drop_column("usuarios", "google_sub")
