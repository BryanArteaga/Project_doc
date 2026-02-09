/*
  Warnings:

  - The `rol` column on the `doctores` table would be dropped and recreated. This will lead to data loss if there is data in the column.

*/
-- AlterTable
ALTER TABLE "doctores" DROP COLUMN "rol",
ADD COLUMN     "rol" "StaffRole" NOT NULL DEFAULT 'DOCTOR';
