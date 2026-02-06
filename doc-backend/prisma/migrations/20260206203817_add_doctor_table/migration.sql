/*
  Warnings:

  - You are about to drop the column `idDoctor` on the `usuarios` table. All the data in the column will be lost.
  - You are about to drop the column `rol` on the `usuarios` table. All the data in the column will be lost.

*/
-- CreateEnum
CREATE TYPE "StaffRole" AS ENUM ('ADMIN', 'DOCTOR');

-- DropForeignKey
ALTER TABLE "usuarios" DROP CONSTRAINT "usuarios_activadoPor_fkey";

-- DropForeignKey
ALTER TABLE "usuarios" DROP CONSTRAINT "usuarios_idDoctorAsignado_fkey";

-- DropIndex
DROP INDEX "usuarios_rol_idx";

-- AlterTable
ALTER TABLE "usuarios" DROP COLUMN "idDoctor",
DROP COLUMN "rol";

-- DropEnum
DROP TYPE "UserRole";

-- CreateTable
CREATE TABLE "doctores" (
    "id" SERIAL NOT NULL,
    "supabaseUserId" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "apellido" TEXT NOT NULL,
    "cedula" TEXT NOT NULL,
    "telefono" TEXT,
    "especialidad" TEXT,
    "rol" "StaffRole" NOT NULL DEFAULT 'DOCTOR',
    "activo" BOOLEAN NOT NULL DEFAULT true,
    "creadoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "actualizadoEn" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "doctores_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "doctores_supabaseUserId_key" ON "doctores"("supabaseUserId");

-- CreateIndex
CREATE UNIQUE INDEX "doctores_email_key" ON "doctores"("email");

-- CreateIndex
CREATE UNIQUE INDEX "doctores_cedula_key" ON "doctores"("cedula");

-- AddForeignKey
ALTER TABLE "usuarios" ADD CONSTRAINT "usuarios_idDoctorAsignado_fkey" FOREIGN KEY ("idDoctorAsignado") REFERENCES "doctores"("id") ON DELETE SET NULL ON UPDATE CASCADE;
