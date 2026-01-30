import 'dotenv/config';
import { PrismaPg } from '@prisma/adapter-pg';
// @ts-ignore - Cliente generado fuera de src
import { PrismaClient } from '../../generated/prisma/client.js';

const connectionString = `${process.env.DATABASE_URL}`;

const adapter = new PrismaPg({ connectionString });
const prisma = new PrismaClient({ adapter });

export { prisma };

