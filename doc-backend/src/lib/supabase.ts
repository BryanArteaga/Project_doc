import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';

// Cargar variables de entorno
dotenv.config();

// Variables de entorno para Supabase
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY;
const supabaseServiceKey = process.env.SUPABASE_SERVICE_KEY;

if (!supabaseUrl || !supabaseAnonKey) {
  throw new Error('Faltan variables de entorno de Supabase: SUPABASE_URL, SUPABASE_ANON_KEY');
}

// Cliente para operaciones públicas (login, register)
export const supabase = createClient(supabaseUrl, supabaseAnonKey);

// Cliente con privilegios de servicio (para operaciones admin)
export const supabaseAdmin = supabaseServiceKey 
  ? createClient(supabaseUrl, supabaseServiceKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    })
  : supabase;

/**
 * Verificar token de Supabase y obtener usuario
 */
export const verifySupabaseToken = async (token: string) => {
  const { data: { user }, error } = await supabase.auth.getUser(token);
  
  if (error || !user) {
    throw new Error('Token inválido o expirado');
  }
  
  return user;
};
