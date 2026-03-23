import { useState } from 'react'
import './App.css'

export default function App() {
  const [currentSlide, setCurrentSlide] = useState(0)

  const features = [
    {
      icon: '📊',
      title: 'Control de Salud',
      description: 'Monitorea glucosa, presión, peso y más'
    },
    {
      icon: '💪',
      title: 'Ejercicio y Nutrición',
      description: 'Seguimiento de actividad física y calorías'
    },
    {
      icon: '�',
      title: 'Notificaciones Inteligentes',
      description: 'Alertas personalizadas por tu condición'
    }
  ]

  const instructions = [
    {
      step: 1,
      title: 'Descargar el APK',
      description: 'Presiona el botón "Descargar APK v1.0" y guarda el archivo',
      images: ['/screenshots/image1.png', '/screenshots/image2.png']
    },
    {
      step: 2,
      title: 'Habilitar Fuentes Desconocidas',
      description: 'Ve a Configuración > Seguridad > Orígenes desconocidos y actívalo',
      images: ['/screenshots/image3.png']
    },
    {
      step: 3,
      title: 'Instalar la App',
      description: 'Abre el archivo APK descargado y presiona "Instalar"',
      images: ['/screenshots/image4.png']
    },
    {
      step: 4,
      title: 'Registrarse',
      description: 'Crea tu cuenta con tu cédula y datos de salud',
      images: ['/screenshots/image5.png']
    },
    {
      step: 5,
      title: 'Comenzar a Monitorear',
      description: 'Registra tus primeras métricas de salud',
      images: ['/screenshots/image6.png']
    }
  ]

  const handleDownloadAPK = () => {
    const link = document.createElement('a')
    link.href = '/app.apk'
    link.download = 'DocControl-v1.0.apk'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <div className="container">
          <div className="logo">
            <img src="/favicon.png" alt="DocControl" className="logo-icon" style={{width: '40px', height: '40px', filter: 'brightness(0.9)'}} />
            <h1>DocControl</h1>
          </div>
          <nav className="nav">
            <a href="#features">Características</a>
            <a href="#instructions">Instrucciones</a>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <section className="hero">
        <div className="container">
          <div className="hero-content">
            <h1>Tu Salud en Tus Manos</h1>
            <p>DocControl es una aplicación de monitoreo de salud que te permite registrar, visualizar y compartir tus datos médicos con tu doctor en tiempo real.</p>
            
            <div className="hero-features">
              <div className="feature-item">
                <span>✅</span>
                <span>Monitoreo sin complicaciones</span>
              </div>
              <div className="feature-item">
                <span>✅</span>
                <span>Comunicación directa con doctor</span>
              </div>
              <div className="feature-item">
                <span>✅</span>
                <span>Historial completo de registros</span>
              </div>
            </div>

            <a onClick={handleDownloadAPK} className="btn btn-primary btn-large" style={{cursor: 'pointer'}}>
              📥 Descargar APK v1.0
            </a>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="features">
        <div className="container">
          <h2>¿Qué Puedes Hacer?</h2>
          <div className="features-grid">
            {features.map((feature, idx) => (
              <div key={idx} className="feature-card">
                <div className="feature-icon">{feature.icon}</div>
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Instructions */}
      <section id="instructions" className="instructions">
        <div className="container">
          <h2>¿Cómo Instalar?</h2>
          <p className="instructions-subtitle">Sigue estos pasos para comenzar</p>
          
          <div className="steps">
            {instructions.map((instruction, idx) => (
              <div key={idx} className="step">
                <div className="step-number">{instruction.step}</div>
                <div className="step-content">
                  <h3>{instruction.title}</h3>
                  <p>{instruction.description}</p>
                </div>
                {instruction.images && instruction.images.length > 0 && (
                  <div className="step-images">
                    {instruction.images.map((image, imgIdx) => (
                      <div key={imgIdx} className="step-image">
                        <img src={image} alt={`${instruction.title} ${imgIdx + 1}`} />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="download-section">
            <h3>¿Listo para Comenzar?</h3>
            <button onClick={handleDownloadAPK} className="btn btn-primary btn-large" style={{cursor: 'pointer', border: 'none'}}>
              📥 Descargar APK Ahora
            </button>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section className="faq">
        <div className="container">
          <h2>Preguntas Frecuentes</h2>
          
          <div className="faq-items">
            <div className="faq-item">
              <h4>⚠️ ¿Qué permisos necesita la app?</h4>
              <p>DocControl requiere acceso a las notificaciones y conexión a internet para sincronizar datos con el servidor.</p>
            </div>

            <div className="faq-item">
              <h4>🔒 ¿Es segura mi información?</h4>
              <p>Sí. Todos tus datos están encriptados y protegidos. Solo tu doctor puede ver tus registros con tu autorización.</p>
            </div>

            <div className="faq-item">
              <h4>💾 ¿Qué pasa si pierdo mi teléfono?</h4>
              <p>Tus registros están guardados en nuestro servidor seguro. Solo inicia sesión en un nuevo dispositivo para recuperar tus datos.</p>
            </div>

            <div className="faq-item">
              <h4>📞 ¿Cuál es la versión mínima de Android?</h4>
              <p>DocControl requiere Android 7.0 o superior.</p>
            </div>

            <div className="faq-item">
              <h4>🆘 ¿Qué hago si la instalación falla?</h4>
              <p>Verifica que tengas suficiente espacio disponible y que los "Orígenes desconocidos" estén habilitados. Contacta a soporte si persiste.</p>
            </div>

            <div className="faq-item">
              <h4>🔄 ¿Con qué frecuencia se actualizará?</h4>
              <p>Lanzamos actualizaciones regularmente con nuevas características. Recibirás notificaciones de actualizaciones disponibles.</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="container">
          <div className="footer-content">
            <div className="footer-section">
              <h4>DocControl</h4>
              <p>Monitoreo de salud inteligente</p>
            </div>
            <div className="footer-section">
              <h4>Enlaces</h4>
              <ul>
                <li><a href="#features">Características</a></li>
                <li><a href="#instructions">Instrucciones</a></li>
              </ul>
            </div>
          </div>
          <div className="footer-bottom">
            <p>&copy; 2026 DocControl. Todos los derechos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
