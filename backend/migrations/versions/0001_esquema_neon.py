"""Esquema base de cuentas y perfiles, idéntico al que ya existe en Neon.

En la base de Neon estas tablas ya existen: márcala como aplicada con
`alembic stamp 0001_esquema_neon` en lugar de ejecutarla.
"""
from alembic import op
import sqlalchemy as sa

revision = "0001_esquema_neon"
down_revision = None
branch_labels = None
depends_on = None

UUID_PK = dict(primary_key=True, server_default=sa.text("uuid_generate_v4()"))


def upgrade():
    op.execute('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"')
    op.create_table(
        "usuarios",
        sa.Column("id", sa.Uuid(), **UUID_PK),
        sa.Column("email", sa.String(255), nullable=False),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("estado", sa.Boolean(), server_default=sa.true(), nullable=False),
        sa.Column("fecha_creacion", sa.DateTime(timezone=True), server_default=sa.func.current_timestamp()),
        sa.Column("google_id", sa.String(255)),
        sa.UniqueConstraint("email", name="usuarios_email_key"),
        sa.UniqueConstraint("google_id", name="usuarios_google_id_key"),
    )
    op.create_index("idx_usuarios_email_search", "usuarios", ["email"])

    roles = op.create_table(
        "roles",
        sa.Column("id", sa.Uuid(), **UUID_PK),
        sa.Column("nombre", sa.String(50), nullable=False),
        sa.UniqueConstraint("nombre", name="roles_nombre_key"),
    )
    op.bulk_insert(roles, [{"nombre": n} for n in ("admin", "cuidador", "paciente")])

    op.create_table(
        "usuario_rol",
        sa.Column("usuario_id", sa.Uuid(), sa.ForeignKey("usuarios.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("rol_id", sa.Uuid(), sa.ForeignKey("roles.id", ondelete="CASCADE"), primary_key=True),
    )
    op.create_table(
        "perfiles",
        sa.Column("id", sa.Uuid(), **UUID_PK),
        sa.Column("usuario_id", sa.Uuid(), sa.ForeignKey("usuarios.id", ondelete="CASCADE"), nullable=False),
        sa.Column("nombre_completo", sa.String(255)),
        sa.Column("telefono", sa.String(20)),
        sa.Column("fecha_nacimiento", sa.Date()),
        sa.Column("tipo_sangre", sa.String(5)),
        sa.UniqueConstraint("usuario_id", name="perfiles_usuario_id_key"),
    )
    op.create_index("idx_perfiles_usuario", "perfiles", ["usuario_id"])
    op.create_table(
        "alergias",
        sa.Column("id", sa.Uuid(), **UUID_PK),
        sa.Column("nombre", sa.String(100), nullable=False),
        sa.Column("categoria", sa.String(50)),
        sa.Column("descripcion", sa.Text()),
        sa.UniqueConstraint("nombre", name="alergias_nombre_key"),
    )
    op.create_table(
        "perfil_alergias",
        sa.Column("perfil_id", sa.Uuid(), sa.ForeignKey("perfiles.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("alergia_id", sa.Uuid(), sa.ForeignKey("alergias.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("severidad", sa.String(50)),
        sa.CheckConstraint(
            "severidad IN ('leve', 'moderada', 'grave')", name="perfil_alergias_severidad_check"
        ),
    )
    op.create_index("idx_perfil_alergias_perfil", "perfil_alergias", ["perfil_id"])
    op.create_table(
        "antecedentes",
        sa.Column("id", sa.Uuid(), **UUID_PK),
        sa.Column("perfil_id", sa.Uuid(), sa.ForeignKey("perfiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("tipo", sa.String(50)),
        sa.Column("descripcion", sa.Text()),
        sa.Column("fecha_registro", sa.DateTime(timezone=True), server_default=sa.func.current_timestamp()),
    )
    op.create_index("idx_antecedentes_perfil", "antecedentes", ["perfil_id"])


def downgrade():
    for table in ("antecedentes", "perfil_alergias", "alergias", "perfiles", "usuario_rol", "roles", "usuarios"):
        op.drop_table(table)
