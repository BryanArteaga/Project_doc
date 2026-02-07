import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Input from '../../components/common/Input';
import PasswordStrengthIndicator from '../../components/common/PasswordStrengthIndicator/PasswordStrengthIndicator';
import { User, Mail, Lock, Stethoscope, ArrowLeft, Phone, CreditCard, Eye, EyeOff } from 'lucide-react';
import toast from 'react-hot-toast';
import { authService } from '../../services/auth.service';
import { isValidEmail, isValidCedula, isValidPhone, isValidTextOnly, isStrongPassword } from '../../utils/security';
import { auditService, AuditAction, AuditLevel } from '../../utils/audit';
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
    confirmPassword: '',
    especialidad: '',
    isAdmin: false
  });
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

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

    // Validación de nombre (solo letras)
    if (!isValidTextOnly(formData.nombre)) {
      toast.error('El nombre solo debe contener letras');
      return;
    }

    // Validación de apellido (solo letras)
    if (!isValidTextOnly(formData.apellido)) {
      toast.error('El apellido solo debe contener letras');
      return;
    }

    // Validación de especialidad (solo letras)
    if (!isValidTextOnly(formData.especialidad)) {
      toast.error('La especialidad solo debe contener letras');
      return;
    }

    // Validación de cédula (10 dígitos)
    if (!isValidCedula(formData.cedula)) {
      toast.error('La cédula debe tener exactamente 10 dígitos');
      return;
    }

    // Validación de teléfono si se proporciona (10 dígitos)
    if (formData.telefono && !isValidPhone(formData.telefono)) {
      toast.error('El teléfono debe tener exactamente 10 dígitos');
      return;
    }

    // Validación de email
    if (!isValidEmail(formData.email)) {
      toast.error('Email inválido');
      return;
    }

    // Validación de contraseña fuerte
    const passwordCheck = isStrongPassword(formData.password);
    if (!passwordCheck.valid) {
      toast.error(passwordCheck.message);
      return;
    }

    // Validación de confirmación de contraseña
    if (formData.password !== formData.confirmPassword) {
      toast.error('Las contraseñas no coinciden');
      return;
    }

    try {
      setLoading(true);
      const { isAdmin, confirmPassword, ...restData } = formData;
      const dataToSend = {
        ...restData,
        rol: isAdmin ? 'ADMIN' : 'DOCTOR',
        ...(restData.telefono ? {} : { telefono: undefined })
      };
      await authService.register(dataToSend);
      
      auditService.log(
        AuditAction.REGISTER_DOCTOR,
        AuditLevel.INFO,
        `Nuevo doctor registrado: ${formData.nombre} ${formData.apellido}`,
        { email: formData.email, rol: dataToSend.rol }
      );
      
      toast.success('Doctor registrado exitosamente');
      navigate('/admin');
    } catch (error: any) {
      auditService.log(
        AuditAction.ERROR,
        AuditLevel.ERROR,
        'Error al registrar doctor',
        { error: error.message, email: formData.email }
      );
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
                placeholder="10 dígitos"
                icon={<CreditCard className="w-5 h-5" />}
                required
                maxLength={10}
              />
              <Input
                label="Teléfono"
                name="telefono"
                value={formData.telefono}
                onChange={handleChange}
                placeholder="10 dígitos (opcional)"
                icon={<Phone className="w-5 h-5" />}
                maxLength={10}
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

            {/* Campo de contraseña con toggle de visibilidad */}
            <Input
              label="Contraseña"
              name="password"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
              onChange={handleChange}
              placeholder="Mín. 8 caracteres, mayúscula, minúscula y número"
              icon={<Lock className="w-5 h-5" />}
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 0,
                    display: 'flex',
                    alignItems: 'center',
                    color: 'var(--color-text-secondary)'
                  }}
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              }
              required
            />

            <PasswordStrengthIndicator password={formData.password} showSuggestions={true} />

            {/* Campo de confirmación de contraseña con toggle de visibilidad */}
            <Input
              label="Confirmar Contraseña"
              name="confirmPassword"
              type={showConfirmPassword ? 'text' : 'password'}
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Vuelve a ingresar la contraseña"
              icon={<Lock className="w-5 h-5" />}
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 0,
                    display: 'flex',
                    alignItems: 'center',
                    color: 'var(--color-text-secondary)'
                  }}
                >
                  {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              }
              required
            />
            
            {formData.confirmPassword && formData.password !== formData.confirmPassword && (
              <p style={{ 
                color: '#dc2626', 
                fontSize: '0.875rem', 
                marginTop: '0.5rem',
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.25rem'
              }}>
                ⚠️ Las contraseñas no coinciden
              </p>
            )}
            {formData.confirmPassword && formData.password === formData.confirmPassword && (
              <p style={{ 
                color: '#16a34a', 
                fontSize: '0.875rem', 
                marginTop: '0.5rem',
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.25rem'
              }}>
                ✓ Las contraseñas coinciden
              </p>
            )}

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
