DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'perfis_identidade'
          AND column_name = 'id'
          AND data_type = 'integer'
    ) THEN
        ALTER TABLE perfis_identidade ALTER COLUMN id TYPE BIGINT;
        ALTER SEQUENCE IF EXISTS perfis_identidade_id_seq AS BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'vinculos_sociais'
          AND column_name = 'id'
          AND data_type = 'integer'
    ) THEN
        ALTER TABLE vinculos_sociais ALTER COLUMN id TYPE BIGINT;
        ALTER SEQUENCE IF EXISTS vinculos_sociais_id_seq AS BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'auditoria_eventos_identidade'
          AND column_name = 'id'
          AND data_type = 'integer'
    ) THEN
        ALTER TABLE auditoria_eventos_identidade ALTER COLUMN id TYPE BIGINT;
        ALTER SEQUENCE IF EXISTS auditoria_eventos_identidade_id_seq AS BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pessoas_identidade'
          AND column_name = 'id'
          AND data_type = 'integer'
    ) THEN
        ALTER TABLE pessoas_identidade ALTER COLUMN id TYPE BIGINT;
        ALTER SEQUENCE IF EXISTS pessoas_identidade_id_seq AS BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pessoas_formas_acesso'
          AND column_name = 'id'
          AND data_type = 'integer'
    ) THEN
        ALTER TABLE pessoas_formas_acesso ALTER COLUMN id TYPE BIGINT;
        ALTER SEQUENCE IF EXISTS pessoas_formas_acesso_id_seq AS BIGINT;
    END IF;
END $$;
