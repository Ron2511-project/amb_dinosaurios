-- =============================================================================
-- SCRIPT 05: SEED DATA — 20 dinosaurios de ejemplo
-- Orden de ejecución: 5to (último, después de triggers y funciones)
-- Descripción: Inserta 20 registros de prueba en dinosaurs_write.
--              El trigger sync_dinosaurs_read los propaga automáticamente
--              a dinosaurs_read con los campos derivados calculados.
-- Estados válidos: ALIVE | EXTINCT | INACTIVE
-- =============================================================================

INSERT INTO dinosaurs_write (name, species, discovery_date, extinction_date, status) VALUES
    ('Tyrannosaurus Rex',      'Theropoda',             '1902-08-12T00:00:00Z', '2025-12-31T23:59:59Z', 'ALIVE'),
    ('Velociraptor',           'Dromaeosauridae',       '1924-05-01T00:00:00Z', '2023-06-15T23:59:59Z', 'EXTINCT'),
    ('Triceratops',            'Ceratopsidae',          '1889-03-20T00:00:00Z', '2026-06-01T00:00:00Z', 'ALIVE'),
    ('Brachiosaurus',          'Sauropoda',             '1900-11-04T00:00:00Z', '2022-01-01T00:00:00Z', 'EXTINCT'),
    ('Stegosaurus',            'Stegosauridae',         '1877-07-10T00:00:00Z', '2023-03-22T23:59:59Z', 'EXTINCT'),
    ('Ankylosaurus',           'Ankylosauria',          '1906-09-15T00:00:00Z', '2026-09-15T00:00:00Z', 'ALIVE'),
    ('Pterodactyl',            'Pterosauria',           '1784-01-01T00:00:00Z', '2021-11-30T23:59:59Z', 'EXTINCT'),
    ('Spinosaurus',            'Spinosauridae',         '1912-04-07T00:00:00Z', '2027-04-07T00:00:00Z', 'ALIVE'),
    ('Diplodocus',             'Sauropoda',             '1877-06-03T00:00:00Z', '2020-08-19T23:59:59Z', 'EXTINCT'),
    ('Parasaurolophus',        'Hadrosauridae',         '1922-10-27T00:00:00Z', '2025-10-27T00:00:00Z', 'ALIVE'),
    ('Allosaurus',             'Allosauridae',          '1877-12-05T00:00:00Z', '2023-09-01T00:00:00Z', 'EXTINCT'),
    ('Iguanodon',              'Ornithopoda',           '1822-02-14T00:00:00Z', '2026-02-14T00:00:00Z', 'INACTIVE'),
    ('Carnotaurus',            'Abelisauridae',         '1984-06-21T00:00:00Z', '2027-06-21T00:00:00Z', 'ALIVE'),
    ('Pachycephalosaurus',     'Marginocephalia',       '1931-08-08T00:00:00Z', '2022-05-17T23:59:59Z', 'EXTINCT'),
    ('Therizinosaurus',        'Therizinosauria',       '1954-03-12T00:00:00Z', '2025-03-12T00:00:00Z', 'INACTIVE'),
    ('Mosasaurus',             'Mosasauridae',          '1764-09-29T00:00:00Z', '2021-07-04T23:59:59Z', 'EXTINCT'),
    ('Gallimimus',             'Ornithomimidae',        '1972-11-18T00:00:00Z', '2026-11-18T00:00:00Z', 'ALIVE'),
    ('Baryonyx',               'Spinosauridae',         '1983-01-30T00:00:00Z', '2025-01-30T00:00:00Z', 'INACTIVE'),
    ('Archaeopteryx',          'Avialae',               '1861-07-22T00:00:00Z', '2020-12-31T23:59:59Z', 'EXTINCT'),
    ('Giganotosaurus',         'Carcharodontosauridae', '1993-09-09T00:00:00Z', '2027-09-09T00:00:00Z', 'ALIVE');

-- Verificación post-insert
DO $$
DECLARE
    write_count INT;
    read_count  INT;
BEGIN
    SELECT COUNT(*) INTO write_count FROM dinosaurs_write;
    SELECT COUNT(*) INTO read_count  FROM dinosaurs_read;
    RAISE NOTICE '✅ Seed completado: % registros en dinosaurs_write, % en dinosaurs_read',
        write_count, read_count;
END
$$;
