-- Fix autoincrement for ejercicios table
-- This resets the sequence to the next available ID

SELECT setval('ejercicios_id_seq', (SELECT MAX(id) FROM ejercicios) + 1);
