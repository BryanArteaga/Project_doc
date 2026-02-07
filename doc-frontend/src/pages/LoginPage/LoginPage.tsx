import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Activity, Lock, Mail, Eye, EyeOff } from 'lucide-react';
import Loading from '../../components/common/Loading';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!email || !password) {
      return;
    }

    setLoading(true);
    try {
      const response = await login({ email, password });
      
      // Redirigir según el rol del usuario
      if (response.doctor.rol === 'ADMIN') {
        navigate('/admin', { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (error) {
      console.error('Error en login:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <Loading message="Iniciando sesión..." fullScreen />;
  }

  return (
    <div className="login-container">
      <div className="login-wrapper">
        {/* Logo y título */}
        <div className="login-logo-section">
          <div className="login-logo">
            <Activity className="w-8 h-8 text-white" />
          </div>
          <h1 className="login-title">Doc System</h1>
          <p className="login-subtitle">Sistema de Gestión de Pacientes</p>
        </div>

        {/* Formulario */}
        <div className="login-card">
          <h2 className="login-card-title">Iniciar Sesión</h2>
          
          <form onSubmit={handleSubmit} className="login-form">
            {/* Email */}
            <div className="login-form-group">
              <label htmlFor="email" className="login-label">
                Correo Electrónico
              </label>
              <div className="login-input-wrapper">
                <Mail className="login-input-icon" />
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEmail(e.target.value)}
                  className="login-input"
                  placeholder="doctor@ejemplo.com"
                  required
                  autoFocus
                />
              </div>
            </div>

            {/* Password */}
            <div className="login-form-group">
              <label htmlFor="password" className="login-label">
                Contraseña
              </label>
              <div className="login-input-wrapper">
                <Lock className="login-input-icon" />
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
                  className="login-input login-input-password"
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="login-password-toggle"
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            {/* Botón */}
            <button
              type="submit"
              disabled={loading}
              className="login-submit-btn"
            >
              {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
            </button>
          </form>

          <div className="login-help-text">
            <p>¿Olvidaste tu contraseña? Contacta al administrador</p>
          </div>
        </div>

        {/* Footer */}
        <div className="login-footer">
          <p>Sistema de gestión médica © 2026</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
