"""Crea cuentas de usuario y sesiones de renovación."""

from alembic import op
import sqlalchemy as sa

revision = "0001_usuarios"
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "usuarios",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("nombre", sa.String(100), nullable=False),
        sa.Column("email", sa.String(255), nullable=False),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("rol", sa.String(10), server_default="paciente", nullable=False),
        sa.Column("creado_en", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("fecha_nacimiento", sa.Date(), nullable=False),
        sa.Column("peso", sa.Numeric(5, 2)),
        sa.Column("altura", sa.Integer()),
        sa.Column("alergias", sa.String(1000), server_default="", nullable=False),
        sa.Column("condiciones_cronicas", sa.String(1000), server_default="", nullable=False),
        sa.Column("contacto_emergencia", sa.String(100), server_default="", nullable=False),
        sa.UniqueConstraint("email", name="uq_usuarios_email"),
        sa.CheckConstraint("email = lower(email)", name="ck_usuarios_email_normalizado"),
        sa.CheckConstraint("rol IN ('paciente', 'cuidador', 'admin')", name="ck_usuarios_rol"),
        sa.CheckConstraint(
            "peso > 0 AND peso <> 'NaN'::numeric", name="ck_usuarios_peso"
        ),
        sa.CheckConstraint("altura > 0", name="ck_usuarios_altura"),
    )
    op.create_table(
        "sesiones_refresh",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("usuario_id", sa.Integer(), sa.ForeignKey("usuarios.id", ondelete="CASCADE"), nullable=False),
        sa.Column("vence_en", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_sesiones_refresh_usuario_id", "sesiones_refresh", ["usuario_id"])
    op.create_index("ix_sesiones_refresh_vence_en", "sesiones_refresh", ["vence_en"])


def downgrade():
    op.drop_table("sesiones_refresh")
    op.drop_table("usuarios")
