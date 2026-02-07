import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import LoginPage from './pages/LoginPage';
import DoctorDashboard from './pages/DoctorDashboard';
import AdminDashboard from './pages/AdminDashboard';
import RegisterDoctor from './pages/RegisterDoctor';
import Reports from './pages/Reports';
import ManagePatients from './pages/ManagePatients';
import ManageDoctors from './pages/ManageDoctors';
import Settings from './pages/Settings';
import ViewPatients from './pages/ViewPatients';
import AuditLogs from './pages/AuditLogs/AuditLogs';
import { auditService, AuditAction, AuditLevel, logUnauthorizedAccess } from './utils/audit';

// Componente para proteger rutas que requieren autenticación
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  
  if (!isAuthenticated) {
    logUnauthorizedAccess(location.pathname);
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
}

// Componente para proteger rutas que requieren rol de admin
function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isAdmin, doctor } = useAuth();
  const location = useLocation();
  
  if (!isAuthenticated) {
    logUnauthorizedAccess(location.pathname);
    return <Navigate to="/login" replace />;
  }
  
  if (!isAdmin) {
    auditService.log(
      AuditAction.UNAUTHORIZED_ACCESS,
      AuditLevel.WARNING,
      `Intento de acceso a ruta de admin sin permisos: ${location.pathname}`,
      { 
        doctorId: doctor?.id,
        doctorRole: doctor?.rol,
        attemptedRoute: location.pathname
      }
    );
    return <Navigate to="/dashboard" replace />;
  }
  
  return <>{children}</>;
}

// Componente para redirigir usuarios autenticados
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isAdmin } = useAuth();
  
  if (isAuthenticated) {
    return <Navigate to={isAdmin ? "/admin" : "/dashboard"} replace />;
  }
  
  return <>{children}</>;
}

function AppRoutes() {
  const { isAuthenticated, isAdmin } = useAuth();

  return (
    <Routes>
      {/* Ruta pública - redirige si ya está autenticado */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />

      {/* Ruta del dashboard del doctor */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DoctorDashboard />
          </ProtectedRoute>
        }
      />

      {/* Rutas del doctor */}
      <Route
        path="/dashboard/patients"
        element={
          <ProtectedRoute>
            <ViewPatients />
          </ProtectedRoute>
        }
      />

      {/* Ruta del dashboard del admin */}
      <Route
        path="/admin"
        element={
          <AdminRoute>
            <AdminDashboard />
          </AdminRoute>
        }
      />

      {/* Rutas de admin */}
      <Route
        path="/admin/register-doctor"
        element={
          <AdminRoute>
            <RegisterDoctor />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/reports"
        element={
          <AdminRoute>
            <Reports />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/manage-patients"
        element={
          <AdminRoute>
            <ManagePatients />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/manage-doctors"
        element={
          <AdminRoute>
            <ManageDoctors />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/audit-logs"
        element={
          <AdminRoute>
            <AuditLogs />
          </AdminRoute>
        }
      />

      {/* Ruta de configuración - accesible para todos los usuarios autenticados */}
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <Settings />
          </ProtectedRoute>
        }
      />

      {/* Ruta raíz - redirige según el estado de autenticación */}
      <Route
        path="/"
        element={
          isAuthenticated ? (
            <Navigate to={isAdmin ? "/admin" : "/dashboard"} replace />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />

      {/* Ruta 404 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <AppRoutes />
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              className: 'dark:bg-gray-800 dark:text-white',
              success: {
                duration: 3000,
                iconTheme: {
                  primary: '#10b981',
                  secondary: '#fff',
                },
              },
              error: {
                duration: 4000,
                iconTheme: {
                  primary: '#ef4444',
                  secondary: '#fff',
                },
              },
            }}
          />
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}

export default App;
