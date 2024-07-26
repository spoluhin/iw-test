DO $$
    DECLARE
        i INTEGER;
    BEGIN
        -- Создание таблиц
        FOR i IN 1..10 LOOP
                EXECUTE format('DROP TABLE IF EXISTS table_%s;', i);

                EXECUTE format('
            CREATE TABLE table_%s (
                id SERIAL PRIMARY KEY,
                col1 VARCHAR(255),
                col2 INTEGER,
                col3 NUMERIC(10,2),
                col4 TIMESTAMP
            );
        ', i);
            END LOOP;

        -- Заполнение таблиц тестовыми данными
        FOR i IN 1..10 LOOP
                EXECUTE format('
            INSERT INTO table_%s (col1, col2, col3, col4)
            SELECT
                md5(random()::text),
                (random() * 100)::INTEGER,
                random() * 1000,
                NOW() - (random() * 100 * INTERVAL ''1 day'')
            FROM generate_series(1, 1000000);
        ', i);
            END LOOP;
    END $$;
