/*
  Warnings:

  - You are about to drop the column `rol` on the `doctores` table. All the data in the column will be lost.
  - You are about to drop the column `dificultad` on the `ejercicios_realizados` table. All the data in the column will be lost.
  - You are about to drop the column `dolor` on the `ejercicios_realizados` table. All the data in the column will be lost.
  - You are about to drop the column `duracionReal` on the `ejercicios_realizados` table. All the data in the column will be lost.
  - You are about to drop the column `notas` on the `ejercicios_realizados` table. All the data in the column will be lost.
  - You are about to drop the column `notas` on the `registros_salud` table. All the data in the column will be lost.
  - You are about to drop the column `peso` on the `registros_salud` table. All the data in the column will be lost.
  - You are about to drop the column `temperatura` on the `registros_salud` table. All the data in the column will be lost.
  - You are about to drop the column `email` on the `usuarios` table. All the data in the column will be lost.
  - You are about to drop the column `password` on the `usuarios` table. All the data in the column will be lost.

*/
-- DropIndex
DROP INDEX "usuarios_email_idx";

-- DropIndex
DROP INDEX "usuarios_email_key";

-- AlterTable
ALTER TABLE "doctores" DROP COLUMN "rol";

-- AlterTable
ALTER TABLE "ejercicios_realizados" DROP COLUMN "dificultad",
DROP COLUMN "dolor",
DROP COLUMN "duracionReal",
DROP COLUMN "notas";

-- AlterTable
ALTER TABLE "registros_salud" DROP COLUMN "notas",
DROP COLUMN "peso",
DROP COLUMN "temperatura";

-- AlterTable
ALTER TABLE "usuarios" DROP COLUMN "email",
DROP COLUMN "password";

-- CreateIndex
CREATE INDEX "idx_doctor_cedula" ON "doctores"("cedula");

-- CreateIndex
CREATE INDEX "idx_doctor_email" ON "doctores"("email");
