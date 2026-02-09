/*
  Warnings:

  - You are about to drop the column `telefonoPersonal` on the `usuarios` table. All the data in the column will be lost.
  - You are about to drop the `asignaciones_ejercicio` table. If the table is not empty, all the data it contains will be lost.

*/
-- DropForeignKey
ALTER TABLE "asignaciones_ejercicio" DROP CONSTRAINT "asignaciones_ejercicio_ejercicioId_fkey";

-- DropForeignKey
ALTER TABLE "asignaciones_ejercicio" DROP CONSTRAINT "asignaciones_ejercicio_pacienteId_fkey";

-- DropForeignKey
ALTER TABLE "ejercicios_realizados" DROP CONSTRAINT "ejercicios_realizados_asignacionId_fkey";

-- AlterTable
ALTER TABLE "doctores" ADD COLUMN     "rol" TEXT;

-- AlterTable
ALTER TABLE "usuarios" DROP COLUMN "telefonoPersonal";

-- DropTable
DROP TABLE "asignaciones_ejercicio";

-- DropEnum
DROP TYPE "ExerciseAssignmentStatus";
