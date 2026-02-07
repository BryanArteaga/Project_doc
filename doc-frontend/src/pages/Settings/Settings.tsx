import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Input from '../../components/common/Input';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, User, Mail, Lock, Bell, Eye, Phone, CreditCard, Info } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import toast from 'react-hot-toast';
import { auditService, AuditAction, AuditLevel } from '../../utils/audit';
import { isValidEmail, isStrongPassword, sanitizeInput } from '../../utils/security';
import './Settings.css';

const Settings: React.FC = () => {
  const navigate = useNavigate();
  const { doctor } = useAuth();
  const [activeTab, setActiveTab] = useState('profile');
  const [loading, setLoading] = useState(false);
  
  const [profileData, setProfileData] = useState({
    nombre: doctor?.nombre || '',
    apellido: doctor?.apellido || '',
    email: doctor?.email || '',
    cedula: doctor?.cedula || '',
    telefono: doctor?.telefono || '',
    especialidad: doctor?.especialidad || ''
  });

  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const [notifications, setNotifications] = useState({
    email: true,
    push: false,
    newPatients: true,
    healthRecords: true
  });

  const handleProfileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setProfileData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPasswordData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handleNotificationChange = (key: string) => {
    setNotifications(prev => ({
      ...prev,
      [key]: !prev[key as keyof typeof notifications]
    }));
  };

  const handleSaveProfile = async () => {
    try {
      setLoading(true);
      
      // Validaciones de seguridad
      if (profileData.email && !isValidEmail(profileData.email)) {
        toast.error('Email inválido');
        return;
      }
      
      if (!profileData.nombre || !profileData.apellido) {
        toast.error('Nombre y apellido son requeridos');
        return;
      }
      
      // Sanitizar inputs
      const sanitizedData = {
        nombre: sanitizeInput(profileData.nombre),
        apellido: sanitizeInput(profileData.apellido),
        email: sanitizeInput(profileData.email),
        cedula: sanitizeInput(profileData.cedula),
        telefono: sanitizeInput(profileData.telefono),
        especialidad: sanitizeInput(profileData.especialidad)
      };
      
      // TODO: Implementar actualización de perfil
      // await doctorService.updateProfile(sanitizedData);
      
      auditService.log(
        AuditAction.UPDATE_SETTINGS,
        AuditLevel.INFO,
        'Perfil actualizado',
        { changes: sanitizedData }
      );
      
      toast.success('Perfil actualizado correctamente (guardado pendiente en backend)');
    } catch (error) {
      auditService.log(
        AuditAction.ERROR,
        AuditLevel.ERROR,
        'Error al actualizar perfil',
        { error }
      );
      toast.error('Error al actualizar perfil');
    } finally {
      setLoading(false);
    }
  };

  const handleSavePassword = async () => {
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      toast.error('Las contraseñas no coinciden');
      return;
    }
    
    // Validación de contraseña fuerte
    const passwordValidation = isStrongPassword(passwordData.newPassword);
    if (!passwordValidation.valid) {
      toast.error(passwordValidation.message);
      return;
    }
    
    if (!passwordData.currentPassword) {
      toast.error('Debe ingresar su contraseña actual');
      return;
    }
    
    try {
      setLoading(true);
      // TODO: Implementar cambio de contraseña
      // await authService.changePassword(passwordData);
      
      auditService.log(
        AuditAction.CHANGE_PASSWORD,
        AuditLevel.INFO,
        'Contraseña actualizada exitosamente'
      );
      
      toast.success('Contraseña actualizada correctamente (guardado pendiente en backend)');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (error) {
      auditService.log(
        AuditAction.ERROR,
        AuditLevel.ERROR,
        'Error al cambiar contraseña',
        { error }
      );
      toast.error('Error al actualizar contraseña');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveNotifications = () => {
    toast.success('Preferencias guardadas');
  };

  return (
    <Layout>
      <PageTransition>
        <div className="settings-container">
          <div className="settings-header">
            <button
              onClick={() => navigate(-1)}
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
            <h1 className="settings-title">Configuración</h1>
            <p className="settings-subtitle">Administra tu cuenta y preferencias</p>
          </div>

        {/* Tabs */}
        <div className="settings-tabs">
          <button
            className={`tab ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => setActiveTab('profile')}
          >
            <User className="w-5 h-5" />
            Perfil
          </button>
          <button
            className={`tab ${activeTab === 'password' ? 'active' : ''}`}
            onClick={() => setActiveTab('password')}
          >
            <Lock className="w-5 h-5" />
            Seguridad
          </button>
          <button
            className={`tab ${activeTab === 'notifications' ? 'active' : ''}`}
            onClick={() => setActiveTab('notifications')}
          >
            <Bell className="w-5 h-5" />
            Notificaciones
          </button>
        </div>

        {/* Content */}
        <div className="settings-content">
          {activeTab === 'profile' && (
            <div className="settings-card">
              <h2 className="card-title">Información Personal</h2>
              <div className="form-grid">
                <Input
                  label="Nombre"
                  name="nombre"
                  value={profileData.nombre}
                  onChange={handleProfileChange}
                  icon={<User className="w-5 h-5" />}
                  required
                />
                <Input
                  label="Apellido"
                  name="apellido"
                  value={profileData.apellido}
                  onChange={handleProfileChange}
                  icon={<User className="w-5 h-5" />}
                  required
                />
                <Input
                  label="Cédula"
                  name="cedula"
                  value={profileData.cedula}
                  onChange={handleProfileChange}
                  icon={<CreditCard className="w-5 h-5" />}
                  disabled
                />
                <Input
                  label="Email"
                  name="email"
                  type="email"
                  value={profileData.email}
                  onChange={handleProfileChange}
                  icon={<Mail className="w-5 h-5" />}
                  disabled
                />
                <Input
                  label="Teléfono"
                  name="telefono"
                  value={profileData.telefono}
                  onChange={handleProfileChange}
                  icon={<Phone className="w-5 h-5" />}
                  placeholder="Número de teléfono"
                />
                <Input
                  label="Especialidad"
                  name="especialidad"
                  value={profileData.especialidad}
                  onChange={handleProfileChange}
                  icon={<Eye className="w-5 h-5" />}
                  placeholder="Ej: Medicina General"
                />
              </div>
              <div className="info-text">
                <Info className="info-icon" />
                <p>La cédula y el email no se pueden modificar por seguridad</p>
              </div>
              <div className="card-actions">
                <button 
                  onClick={handleSaveProfile} 
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
                    transition: 'all 0.3s ease',
                    boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
                  }}
                  onMouseEnter={(e) => {
                    if (!loading) e.currentTarget.style.backgroundColor = '#4A52C4';
                  }}
                  onMouseLeave={(e) => {
                    if (!loading) e.currentTarget.style.backgroundColor = '#5B65DC';
                  }}
                >
                  {loading ? 'Guardando...' : 'Guardar Cambios'}
                </button>
              </div>
            </div>
          )}

          {activeTab === 'password' && (
            <div className="settings-card">
              <h2 className="card-title">Cambiar Contraseña</h2>
              <div className="form-grid">
                <Input
                  label="Contraseña Actual"
                  name="currentPassword"
                  type="password"
                  value={passwordData.currentPassword}
                  onChange={handlePasswordChange}
                  icon={<Lock className="w-5 h-5" />}
                />
                <Input
                  label="Nueva Contraseña"
                  name="newPassword"
                  type="password"
                  value={passwordData.newPassword}
                  onChange={handlePasswordChange}
                  icon={<Lock className="w-5 h-5" />}
                />
                <Input
                  label="Confirmar Contraseña"
                  name="confirmPassword"
                  type="password"
                  value={passwordData.confirmPassword}
                  onChange={handlePasswordChange}
                  icon={<Lock className="w-5 h-5" />}
                />
              </div>
              <div className="card-actions">
                <button 
                  onClick={handleSavePassword} 
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
                    transition: 'all 0.3s ease',
                    boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
                  }}
                  onMouseEnter={(e) => {
                    if (!loading) e.currentTarget.style.backgroundColor = '#4A52C4';
                  }}
                  onMouseLeave={(e) => {
                    if (!loading) e.currentTarget.style.backgroundColor = '#5B65DC';
                  }}
                >
                  {loading ? 'Actualizando...' : 'Actualizar Contraseña'}
                </button>
              </div>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div className="settings-card">
              <h2 className="card-title">Preferencias de Notificaciones</h2>
              <div className="notifications-list">
                <div className="notification-item">
                  <div>
                    <h3 className="notification-title">Notificaciones por Email</h3>
                    <p className="notification-desc">Recibir notificaciones en tu correo</p>
                  </div>
                  <label className="switch">
                    <input
                      type="checkbox"
                      checked={notifications.email}
                      onChange={() => handleNotificationChange('email')}
                    />
                    <span className="slider"></span>
                  </label>
                </div>
                <div className="notification-item">
                  <div>
                    <h3 className="notification-title">Notificaciones Push</h3>
                    <p className="notification-desc">Recibir notificaciones en el navegador</p>
                  </div>
                  <label className="switch">
                    <input
                      type="checkbox"
                      checked={notifications.push}
                      onChange={() => handleNotificationChange('push')}
                    />
                    <span className="slider"></span>
                  </label>
                </div>
                <div className="notification-item">
                  <div>
                    <h3 className="notification-title">Nuevos Pacientes</h3>
                    <p className="notification-desc">Alertas cuando se registren nuevos pacientes</p>
                  </div>
                  <label className="switch">
                    <input
                      type="checkbox"
                      checked={notifications.newPatients}
                      onChange={() => handleNotificationChange('newPatients')}
                    />
                    <span className="slider"></span>
                  </label>
                </div>
                <div className="notification-item">
                  <div>
                    <h3 className="notification-title">Registros de Salud</h3>
                    <p className="notification-desc">Notificar cuando haya nuevos registros</p>
                  </div>
                  <label className="switch">
                    <input
                      type="checkbox"
                      checked={notifications.healthRecords}
                      onChange={() => handleNotificationChange('healthRecords')}
                    />
                    <span className="slider"></span>
                  </label>
                </div>
              </div>
              <div className="card-actions">
                <button 
                  onClick={handleSaveNotifications}
                  style={{
                    backgroundColor: '#5B65DC',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1rem',
                    borderRadius: '0.5rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontSize: '1rem',
                    transition: 'all 0.3s ease',
                    boxShadow: '0 4px 20px rgba(91, 101, 220, 0.15)'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor = '#4A52C4';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = '#5B65DC';
                  }}
                >
                  Guardar Preferencias
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
      </PageTransition>
    </Layout>
  );
};

export default Settings;
