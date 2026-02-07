import React from 'react';

interface ErrorBoundaryState {
  hasError: boolean;
  error?: Error;
  errorInfo?: React.ErrorInfo;
}

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error capturado por ErrorBoundary:', error, errorInfo);
    
    // Guardar errores en localStorage para revisión posterior
    try {
      const errors = JSON.parse(localStorage.getItem('app_errors') || '[]');
      const errorLog = {
        message: error.message,
        stack: error.stack,
        componentStack: errorInfo.componentStack,
        timestamp: new Date().toISOString(),
        url: window.location.href
      };
      
      errors.push(errorLog);
      
      // Mantener solo los últimos 10 errores
      if (errors.length > 10) {
        errors.shift();
      }
      
      localStorage.setItem('app_errors', JSON.stringify(errors));
    } catch (storageError) {
      console.error('No se pudo guardar el error en localStorage:', storageError);
    }

    this.setState({ error, errorInfo });
  }

  handleReload = () => {
    window.location.reload();
  };

  handleReset = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-brand-lighter dark:bg-gray-900 p-4">
          <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-lg shadow-xl p-8 text-center">
            <div className="text-6xl mb-4">😔</div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
              Algo salió mal
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Se ha producido un error inesperado. Por favor, recarga la página.
            </p>
            
            <div className="flex gap-3 justify-center">
              <button
                onClick={this.handleReload}
                className="px-6 py-3 bg-brand-main text-white rounded-lg hover:bg-brand-dark transition-colors"
              >
                Recargar Página
              </button>
              <button
                onClick={this.handleReset}
                className="px-6 py-3 bg-gray-200 dark:bg-gray-700 text-gray-900 dark:text-white rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors"
              >
                Intentar de Nuevo
              </button>
            </div>

            {import.meta.env.DEV && this.state.error && (
              <details className="mt-6 text-left">
                <summary className="cursor-pointer text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white">
                  Ver detalles del error (solo en desarrollo)
                </summary>
                <div className="mt-3 p-4 bg-red-50 dark:bg-red-900/20 rounded-lg">
                  <p className="text-sm font-bold text-red-800 dark:text-red-300 mb-2">
                    {this.state.error.message}
                  </p>
                  <pre className="text-xs text-red-700 dark:text-red-400 overflow-auto max-h-64">
                    {this.state.error.stack}
                  </pre>
                  {this.state.errorInfo && (
                    <>
                      <p className="text-sm font-bold text-red-800 dark:text-red-300 mt-4 mb-2">
                        Component Stack:
                      </p>
                      <pre className="text-xs text-red-700 dark:text-red-400 overflow-auto max-h-64">
                        {this.state.errorInfo.componentStack}
                      </pre>
                    </>
                  )}
                </div>
              </details>
            )}

            <p className="text-xs text-gray-500 dark:text-gray-500 mt-6">
              El error ha sido registrado localmente. Puedes revisar los errores guardados en la consola del navegador con:
              <code className="block mt-2 p-2 bg-gray-100 dark:bg-gray-700 rounded text-left">
                JSON.parse(localStorage.getItem('app_errors') || '[]')
              </code>
            </p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
