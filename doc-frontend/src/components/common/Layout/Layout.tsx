import React, { useState, type ReactNode } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { LogOut, Settings } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Modal from '../Modal';
import Loading from '../Loading';
import ThemeToggle from '../ThemeToggle';
import './Layout.css';

interface LayoutProps {
  children: ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { doctor, isAdmin, logout, loggingOut } = useAuth();
  const navigate = useNavigate();
  const [showLogoutModal, setShowLogoutModal] = useState(false);

  const handleLogoutClick = () => {
    setShowLogoutModal(true);
  };

  const confirmLogout = async () => {
    setShowLogoutModal(false);
    await logout();
    navigate('/login', { replace: true });
  };

  if (loggingOut) {
    return <Loading message="Cerrando sesión..." fullScreen />;
  }

  return (
    <div className="layout-container">
      {/* Header */}
      <header className="layout-header">
        <div className="layout-header-content">
          <div className="layout-header-inner">
            {/* Logo/Título */}
            <div className="layout-logo-section">
              <img src="/favicon.png" alt="DocControl" style={{width: '40px', height: '40px', marginRight: '12px'}} />
              <h1 className="layout-logo-title">DocControl</h1>
              {isAdmin && (
                <span className="layout-admin-badge">
                  Admin
                </span>
              )}
            </div>

            {/* Usuario y acciones */}
            <div className="layout-user-section">
              {/* Información del usuario */}
              <div className="layout-user-info">
                <div className="layout-user-details">
                  <p className="layout-user-name">
                    {doctor?.nombre} {doctor?.apellido}
                  </p>
                  <p className="layout-user-role">{doctor?.especialidad || 'Doctor'}</p>
                </div>
                <div className="layout-user-avatar">
                  {doctor?.nombre?.charAt(0)}{doctor?.apellido?.charAt(0)}
                </div>
              </div>

              <div className="layout-actions">
                <ThemeToggle />
                <button
                  onClick={() => navigate('/settings')}
                  className="layout-action-btn"
                  title="Configuración"
                >
                  <Settings className="w-5 h-5" />
                </button>
                <button
                  onClick={handleLogoutClick}
                  className="layout-action-btn layout-action-btn-logout"
                  title="Cerrar sesión"
                >
                  <LogOut className="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Contenido principal */}
      <main className="layout-main">{children}</main>

      {/* Footer */}
      <footer className="layout-footer">
        <div className="layout-footer-content">
          <p className="layout-footer-text">
            © 2026 DocControl. Todos los derechos reservados.
          </p>
        </div>
      </footer>

      {/* Modal de confirmación de logout */}
      <Modal
        isOpen={showLogoutModal}
        onClose={() => setShowLogoutModal(false)}
        title="Cerrar sesión"
      >
        <div style={{ padding: '0' }}>
          <p style={{ fontSize: '1rem', marginBottom: '1.5rem', color: '#64748b', textAlign: 'center' }}>
            ¿Está seguro que desea cerrar sesión?
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
            <button
              onClick={confirmLogout}
              style={{
                backgroundColor: '#5B65DC',
                color: '#ffffff',
                border: 'none',
                padding: '0.5rem 1rem',
                borderRadius: '0.5rem',
                fontWeight: 600,
                cursor: 'pointer',
                fontSize: '1rem',
                transition: 'all 0.3s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#4A52C4'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#5B65DC'}
            >
              Sí, cerrar sesión
            </button>
            <button
              onClick={() => setShowLogoutModal(false)}
              style={{
                backgroundColor: '#64748b',
                color: '#ffffff',
                border: 'none',
                padding: '0.5rem 1rem',
                borderRadius: '0.5rem',
                fontWeight: 600,
                cursor: 'pointer',
                fontSize: '1rem',
                transition: 'all 0.3s ease'
              }}
              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#475569'}
              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = '#64748b'}
            >
              No, cancelar
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Layout;
