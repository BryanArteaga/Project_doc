import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Input from '../../components/common/Input';
import { User, Mail, Lock, Stethoscope, ArrowLeft, Phone } from 'lucide-react';
import toast from 'react-hot-toast';
import { authService } from '../../services/auth.service';
import './RegisterDoctor.css';

const RegisterDoctor: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    cedula: '',
    telefono: '',
    email: '',
    password: '',
    especialidad: '',
    isAdmin: false
  });
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.nombre || !formData.apellido || !formData.cedula || !formData.email || !formData.password || !formData.especialidad) {
      toast.error('Por favor completa todos los campos');
      return;
    }

    try {
      setLoading(true);
      const { isAdmin, ...restData } = formData;
      const dataToSend = {
        ...restData,
        rol: isAdmin ? 'ADMIN' : 'DOCTOR',
        ...(restData.telefono ? {} : { telefono: undefined })
      };
      await authService.register(dataToSend);
      toast.success('Doctor registrado exitosamente');
      navigate('/admin');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Error al registrar doctor');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="register-doctor-container">
        <div className="register-doctor-header">
          <button
            onClick={() => navigate('/admin')}
            style={{
              backgroundColor: '#5B65DC',
              color: '#ffffff',
              border: 'none',
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              fontWeight: 600,
              cursor: 'pointer',
              fontSize: '1rem',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
            }}
            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
          >
            <ArrowLeft style={{ width: '1rem', height: '1rem' }} />
            Volver
          </button>
          <h1 className="register-doctor-title">Registrar Nuevo Doctor</h1>
          <p className="register-doctor-subtitle">Completa la información del nuevo doctor</p>
        </div>

        <div className="register-doctor-card">
          <form onSubmit={handleSubmit} className="register-doctor-form">
            <div className="form-row">
              <Input
                label="Nombre"
                name="nombre"
                value={formData.nombre}
                onChange={handleChange}
                placeholder="Ingresa el nombre"
                icon={<User className="w-5 h-5" />}
                required
              />
              <Input
                label="Apellido"
                name="apellido"
                value={formData.apellido}
                onChange={handleChange}
                placeholder="Ingresa el apellido"
                icon={<User className="w-5 h-5" />}
                required
              />
            </div>

            <div className="form-row">
              <Input
                label="Cédula"
                name="cedula"
                value={formData.cedula}
                onChange={handleChange}
                placeholder="Ingresa la cédula"
                icon={<User className="w-5 h-5" />}
                required
              />
              <Input
                label="Teléfono"
                name="telefono"
                value={formData.telefono}
                onChange={handleChange}
                placeholder="Opcional"
                icon={<Phone className="w-5 h-5" />}
              />
            </div>

            <div className="form-row">
              <Input
                label="Especialidad"
                name="especialidad"
                value={formData.especialidad}
                onChange={handleChange}
                placeholder="Ej: Cardiología, Pediatría, etc."
                icon={<Stethoscope className="w-5 h-5" />}
                required
              />
              <Input
                label="Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="doctor@ejemplo.com"
                icon={<Mail className="w-5 h-5" />}
                required
              />
            </div>

            <Input
              label="Contraseña"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="••••••••"
              icon={<Lock className="w-5 h-5" />}
              required
            />

            <div className="checkbox-container">
              <input
                type="checkbox"
                id="isAdmin"
                name="isAdmin"
                checked={formData.isAdmin}
                onChange={handleChange}
                className="checkbox-input"
              />
              <label htmlFor="isAdmin" className="checkbox-label">
                Otorgar permisos de administrador
              </label>
            </div>

            <div className="form-actions">
              <button
                type="submit"
                disabled={loading}
                style={{
                  backgroundColor: '#5B65DC',
                  color: '#ffffff',
                  border: 'none',
                  padding: '0.5rem 1rem',
                  borderRadius: '0.5rem',
                  fontWeight: 600,
                  cursor: loading ? 'not-allowed' : 'pointer',
                  opacity: loading ? 0.5 : 1,
                  fontSize: '1rem',
                  width: '100%',
                  transition: 'all 0.3s ease',
                  boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
                }}
                onMouseEnter={(e) => { if (!loading) e.currentTarget.style.backgroundColor = '#4A52C4'; }}
                onMouseLeave={(e) => { if (!loading) e.currentTarget.style.backgroundColor = '#5B65DC'; }}
              >
                {loading ? 'Registrando...' : 'Registrar Doctor'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Layout>
  );
};

export default RegisterDoctor;
