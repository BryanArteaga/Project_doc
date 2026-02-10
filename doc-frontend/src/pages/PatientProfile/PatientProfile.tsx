import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, User, Activity, TrendingUp, Calendar, ChevronDown, ChevronUp } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../../services/api';
import './PatientProfile.css';

interface PatientData {
    id: number;
    nombre: string;
    apellido: string;
    cedula?: string;
    fechaNacimiento?: string;
    edad?: number;
    peso?: number;
    altura?: number;
    sexo?: string;
    enfermedad?: string;
    colorEnfermedad?: string;
    estadoCuenta: string;
    registradoEn: string;
    ultimaSincronizacion?: string;
    registrosSalud: Array<{
        id: number;
        presionSistolica?: number;
        presionDiastolica?: number;
        glucosa?: number;
        peso?: number;
        ejercicio?: number;
        calorias?: number;
        estadoAnimo?: string;
        notas?: string;
        registradoEn: string;
    }>;
    ejerciciosRealizados: Array<{
        id: number;
        completadoEn: string;
        ejercicio: {
            nombre: string;
            descripcion?: string;
            duracion?: number;
        };
    }>;
}

const PatientProfile: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [data, setData] = useState<PatientData | null>(null);
    const [loading, setLoading] = useState(true);
    const [expandedStats, setExpandedStats] = useState(false);
    const [expandedHealth, setExpandedHealth] = useState(false);
    const [expandedExercises, setExpandedExercises] = useState(false);

    useEffect(() => {
        loadPatientData();
    }, [id]);

    const loadPatientData = async () => {
        try {
            setLoading(true);
            // Usar el servicio api que ya tiene el token configurado
            const response = await api.get(`/api/doctor/patients/${id}`);
            setData(response.data);
        } catch (error: any) {
            console.error('Error al cargar perfil del paciente:', error);
            toast.error('Error al cargar perfil del paciente');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <Layout>
                <Loading message="Cargando perfil del paciente..." fullHeight />
            </Layout>
        );
    }

    if (!data) {
        return (
            <Layout>
                <div className="error-state">
                    <p>No se pudo cargar el perfil del paciente</p>
                    <button onClick={() => navigate('/dashboard/patients')}>Volver a pacientes</button>
                </div>
            </Layout>
        );
    }

    const { registrosSalud, ejerciciosRealizados } = data;
    const patient = data;

    // Calcular estadísticas
    const totalEjercicios = ejerciciosRealizados.length;

    // Agrupar ejercicios por nombre
    const ejerciciosPorNombre = ejerciciosRealizados.reduce((acc: any, ejercicio) => {
        const nombre = ejercicio.ejercicio.nombre;
        if (!acc[nombre]) {
            acc[nombre] = {
                nombre: nombre,
                descripcion: ejercicio.ejercicio.descripcion || '',
                duracion: ejercicio.ejercicio.duracion || 0,
                completaciones: []
            };
        }
        acc[nombre].completaciones.push(ejercicio);
        return acc;
    }, {});

    const latestHealth = registrosSalud[0];

    // Color por enfermedad (coinciden con la app Android)
    const enfermedadColor = patient.enfermedad === 'Hipertensión' ? '#D32F2F' :
        patient.enfermedad === 'Diabetes' ? '#FFF176' :
            patient.enfermedad === 'Obesidad' ? '#81D4FA' :
                patient.enfermedad === 'Colesterol alto' ? '#FFA726' : '#6b7280';

    return (
        <Layout>
            <PageTransition>
                <div className="patient-profile-container">
                    {/* Header */}
                    <div className="profile-header">
                        <button
                            onClick={() => navigate('/dashboard/patients')}
                            className="back-button"
                        >
                            <ArrowLeft />
                            Volver
                        </button>
                        <div style={{ flex: 1, textAlign: 'center' }}>
                            <h1 className="profile-title">
                                {patient.nombre} {patient.apellido}
                            </h1>
                            {patient.enfermedad && (
                                <span style={{
                                    display: 'inline-block',
                                    padding: '0.5rem 1rem',
                                    background: enfermedadColor,
                                    color: 'white',
                                    borderRadius: '999px',
                                    fontWeight: 600,
                                    fontSize: '0.875rem',
                                    marginTop: '0.5rem'
                                }}>
                                    {patient.enfermedad}
                                </span>
                            )}
                        </div>
                    </div>

                    {/* Información Básica */}
                    <div className="profile-grid">
                        <div className="info-card">
                            <div className="card-icon">
                                <User />
                            </div>
                            <h3>Información Personal</h3>
                            <div className="info-list">
                                {patient.cedula && <div className="info-item"><span>Cédula:</span><span>{patient.cedula}</span></div>}
                                {patient.edad && <div className="info-item"><span>Edad:</span><span>{patient.edad} años</span></div>}
                                {patient.sexo && <div className="info-item"><span>Sexo:</span><span>{patient.sexo}</span></div>}
                                {patient.peso && <div className="info-item"><span>Peso:</span><span>{patient.peso} kg</span></div>}
                                {patient.altura && <div className="info-item"><span>Altura:</span><span>{patient.altura} cm</span></div>}
                                {patient.enfermedad && <div className="info-item"><span>Enfermedad:</span><span>{patient.enfermedad}</span></div>}
                            </div>
                        </div>

                        {/* Estadísticas */}
                        <div className="stats-card">
                            <div className="card-icon">
                                <TrendingUp />
                            </div>
                            <h3>Resumen de Actividad</h3>
                            <div className="stats-grid">
                                <div className="stat-item">
                                    <div className="stat-value">{registrosSalud.length}</div>
                                    <div className="stat-label">Registros de Salud</div>
                                </div>
                                <div className="stat-item">
                                    <div className="stat-value">{totalEjercicios}</div>
                                    <div className="stat-label">Ejercicios Completados</div>
                                </div>
                                <div className="stat-item">
                                    <div className="stat-value">{Object.keys(ejerciciosPorNombre).length}</div>
                                    <div className="stat-label">Tipos de Ejercicios</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Último Registro de Salud */}
                    {latestHealth && (
                        <div className="health-card">
                            <div className="card-header">
                                <Activity />
                                <h3>Último Registro de Salud</h3>
                                <span className="date">
                                    {new Date(latestHealth.registradoEn).toLocaleDateString()}
                                </span>
                            </div>
                            <div className="health-metrics">
                                {/* Hipertensión - Mostrar presión arterial */}
                                {patient.enfermedad === 'Hipertensión' && latestHealth.presionSistolica && latestHealth.presionDiastolica && (
                                    <div className="metric">
                                        <span className="metric-label">Presión Arterial</span>
                                        <span className="metric-value">{latestHealth.presionSistolica}/{latestHealth.presionDiastolica} mmHg</span>
                                    </div>
                                )}
                                {/* Diabetes - Mostrar glucosa */}
                                {patient.enfermedad === 'Diabetes' && latestHealth.glucosa && (
                                    <div className="metric">
                                        <span className="metric-label">Glucosa</span>
                                        <span className="metric-value">{latestHealth.glucosa} mg/dL</span>
                                    </div>
                                )}
                                {/* Obesidad - Mostrar peso y presión */}
                                {patient.enfermedad === 'Obesidad' && (
                                    <>
                                        {latestHealth.peso && (
                                            <div className="metric">
                                                <span className="metric-label">Peso</span>
                                                <span className="metric-value">{latestHealth.peso} kg</span>
                                            </div>
                                        )}
                                        {latestHealth.presionSistolica && latestHealth.presionDiastolica && (
                                            <div className="metric">
                                                <span className="metric-label">Presión Arterial</span>
                                                <span className="metric-value">{latestHealth.presionSistolica}/{latestHealth.presionDiastolica} mmHg</span>
                                            </div>
                                        )}
                                    </>
                                )}
                                {/* Colesterol alto - Mostrar glucosa y presión */}
                                {patient.enfermedad === 'Colesterol alto' && (
                                    <>
                                        {latestHealth.glucosa && (
                                            <div className="metric">
                                                <span className="metric-label">Glucosa</span>
                                                <span className="metric-value">{latestHealth.glucosa} mg/dL</span>
                                            </div>
                                        )}
                                        {latestHealth.presionSistolica && latestHealth.presionDiastolica && (
                                            <div className="metric">
                                                <span className="metric-label">Presión Arterial</span>
                                                <span className="metric-value">{latestHealth.presionSistolica}/{latestHealth.presionDiastolica} mmHg</span>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Estadísticas Clínicas - Colapsable */}
                    {registrosSalud.length > 0 && (() => {
                        const presiones = registrosSalud.filter(r => r.presionSistolica && r.presionDiastolica);
                        const glucosas = registrosSalud.filter(r => r.glucosa);
                        const pesos = registrosSalud.filter(r => r.peso);
                        const ejercicios = registrosSalud.filter(r => r.ejercicio);
                        const calorias = registrosSalud.filter(r => r.calorias);

                        const calcularPromedio = (arr: number[]) => arr.length > 0 ? Math.round(arr.reduce((a, b) => a + b, 0) / arr.length) : 0;

                        return (
                            <div className="collapsible-section">
                                <div className="section-header" onClick={() => setExpandedStats(!expandedStats)}>
                                    <div className="section-title">
                                        <TrendingUp />
                                        <h3>Estadísticas Clínicas</h3>
                                    </div>
                                    {expandedStats ? <ChevronUp /> : <ChevronDown />}
                                </div>
                                {expandedStats && (
                                    <div className="section-content">
                                        <div className="clinical-stats-grid">
                                            {patient.enfermedad === 'Hipertensión' && presiones.length > 0 && (
                                                <div className="stat-box">
                                                    <h4>Presión Arterial</h4>
                                                    <div className="stat-values">
                                                        <div className="stat-item">
                                                            <span className="stat-label">Promedio</span>
                                                            <span className="stat-number">{calcularPromedio(presiones.map(r => r.presionSistolica!))}/{calcularPromedio(presiones.map(r => r.presionDiastolica!))}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Mínima</span>
                                                            <span className="stat-number">{Math.min(...presiones.map(r => r.presionSistolica!))}/{Math.min(...presiones.map(r => r.presionDiastolica!))}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Máxima</span>
                                                            <span className="stat-number">{Math.max(...presiones.map(r => r.presionSistolica!))}/{Math.max(...presiones.map(r => r.presionDiastolica!))}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}
                                            {patient.enfermedad === 'Diabetes' && glucosas.length > 0 && (
                                                <div className="stat-box">
                                                    <h4>Glucosa (mg/dL)</h4>
                                                    <div className="stat-values">
                                                        <div className="stat-item">
                                                            <span className="stat-label">Promedio</span>
                                                            <span className="stat-number">{calcularPromedio(glucosas.map(r => r.glucosa!))}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Mínima</span>
                                                            <span className="stat-number">{Math.min(...glucosas.map(r => r.glucosa!))}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Máxima</span>
                                                            <span className="stat-number">{Math.max(...glucosas.map(r => r.glucosa!))}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}
                                            {patient.enfermedad === 'Obesidad' && pesos.length > 0 && (
                                                <div className="stat-box">
                                                    <h4>Peso (kg)</h4>
                                                    <div className="stat-values">
                                                        <div className="stat-item">
                                                            <span className="stat-label">Promedio</span>
                                                            <span className="stat-number">{(pesos.reduce((sum, r) => sum + r.peso!, 0) / pesos.length).toFixed(1)}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Mínimo</span>
                                                            <span className="stat-number">{Math.min(...pesos.map(r => r.peso!))}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Máximo</span>
                                                            <span className="stat-number">{Math.max(...pesos.map(r => r.peso!))}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}
                                            {ejercicios.length > 0 && (
                                                <div className="stat-box">
                                                    <h4>Actividad Física</h4>
                                                    <div className="stat-values">
                                                        <div className="stat-item">
                                                            <span className="stat-label">Total Minutos</span>
                                                            <span className="stat-number">{ejercicios.reduce((sum, r) => sum + r.ejercicio!, 0)}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Promedio/día</span>
                                                            <span className="stat-number">{calcularPromedio(ejercicios.map(r => r.ejercicio!))}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}
                                            {calorias.length > 0 && (
                                                <div className="stat-box">
                                                    <h4>Calorías Consumidas</h4>
                                                    <div className="stat-values">
                                                        <div className="stat-item">
                                                            <span className="stat-label">Total</span>
                                                            <span className="stat-number">{calorias.reduce((sum, r) => sum + r.calorias!, 0)}</span>
                                                        </div>
                                                        <div className="stat-item">
                                                            <span className="stat-label">Promedio/día</span>
                                                            <span className="stat-number">{calcularPromedio(calorias.map(r => r.calorias!))}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })()}

                    {/* Historial de Registros - Colapsable */}
                    <div className="collapsible-section">
                        <div className="section-header" onClick={() => setExpandedHealth(!expandedHealth)}>
                            <div className="section-title">
                                <Calendar />
                                <h3>Registros de Salud ({registrosSalud.length})</h3>
                            </div>
                            {expandedHealth ? <ChevronUp /> : <ChevronDown />}
                        </div>
                        {expandedHealth && (
                            <div className="section-content">
                                {registrosSalud.length === 0 ? (
                                    <p className="empty-message">No hay registros de salud disponibles</p>
                                ) : (
                                    <div className="history-table">
                                        <table>
                                            <thead>
                                                <tr>
                                                    <th>#</th>
                                                    <th>Fecha</th>
                                                    <th>Hora</th>
                                                    {/* Hipertensión - solo presión */}
                                                    {patient.enfermedad === 'Hipertensión' && <th>Presión Arterial</th>}
                                                    {/* Diabetes - solo glucosa */}
                                                    {patient.enfermedad === 'Diabetes' && <th>Glucosa (mg/dL)</th>}
                                                    {/* Obesidad - peso y presión */}
                                                    {patient.enfermedad === 'Obesidad' && (
                                                        <>
                                                            <th>Peso (kg)</th>
                                                            <th>Presión Arterial</th>
                                                        </>
                                                    )}
                                                    {/* Colesterol alto - glucosa y presión */}
                                                    {patient.enfermedad === 'Colesterol alto' && (
                                                        <>
                                                            <th>Glucosa (mg/dL)</th>
                                                            <th>Presión Arterial</th>
                                                        </>
                                                    )}
                                                    {/* Columnas para todas las enfermedades */}
                                                    <th>Min. Ejercicio</th>
                                                    <th>Calorías</th>
                                                    <th>Estado de Ánimo</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {registrosSalud.map((registro, index) => {
                                                    const fecha = new Date(registro.registradoEn);
                                                    return (
                                                        <tr key={registro.id}>
                                                            <td style={{ fontWeight: 700, textAlign: 'center', background: 'var(--bg-secondary)' }}>{registrosSalud.length - index}</td>
                                                            <td style={{ fontWeight: 600 }}>{fecha.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                                                            <td style={{ fontSize: '0.875rem' }}>{fecha.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}</td>
                                                            {/* Hipertensión */}
                                                            {patient.enfermedad === 'Hipertensión' && (
                                                                <td style={{ fontSize: '1.05rem' }}>
                                                                    {registro.presionSistolica && registro.presionDiastolica
                                                                        ? <><strong style={{ color: '#5B65DC' }}>{registro.presionSistolica}</strong>/<strong style={{ color: '#5B65DC' }}>{registro.presionDiastolica}</strong> mmHg</>
                                                                        : <span style={{ color: '#9ca3af', fontStyle: 'italic' }}>Sin datos</span>}
                                                                </td>
                                                            )}
                                                            {/* Diabetes */}
                                                            {patient.enfermedad === 'Diabetes' && (
                                                                <td style={{ fontSize: '1.05rem' }}>
                                                                    {registro.glucosa ? <><strong style={{ color: '#5B65DC' }}>{registro.glucosa}</strong> mg/dL</> : <span style={{ color: '#9ca3af', fontStyle: 'italic' }}>Sin datos</span>}
                                                                </td>
                                                            )}
                                                            {/* Obesidad */}
                                                            {patient.enfermedad === 'Obesidad' && (
                                                                <>
                                                                    <td style={{ fontSize: '1.05rem' }}>
                                                                        {registro.peso ? <><strong style={{ color: '#5B65DC' }}>{registro.peso}</strong> kg</> : <span style={{ color: '#9ca3af' }}>-</span>}
                                                                    </td>
                                                                    <td style={{ fontSize: '1.05rem' }}>
                                                                        {registro.presionSistolica && registro.presionDiastolica
                                                                            ? <><strong style={{ color: '#5B65DC' }}>{registro.presionSistolica}</strong>/<strong style={{ color: '#5B65DC' }}>{registro.presionDiastolica}</strong></>
                                                                            : <span style={{ color: '#9ca3af' }}>-</span>}
                                                                    </td>
                                                                </>
                                                            )}
                                                            {/* Colesterol alto */}
                                                            {patient.enfermedad === 'Colesterol alto' && (
                                                                <>
                                                                    <td style={{ fontSize: '1.05rem' }}>
                                                                        {registro.glucosa ? <><strong style={{ color: '#5B65DC' }}>{registro.glucosa}</strong> mg/dL</> : <span style={{ color: '#9ca3af' }}>-</span>}
                                                                    </td>
                                                                    <td style={{ fontSize: '1.05rem' }}>
                                                                        {registro.presionSistolica && registro.presionDiastolica
                                                                            ? <><strong style={{ color: '#5B65DC' }}>{registro.presionSistolica}</strong>/<strong style={{ color: '#5B65DC' }}>{registro.presionDiastolica}</strong></>
                                                                            : <span style={{ color: '#9ca3af' }}>-</span>}
                                                                    </td>
                                                                </>
                                                            )}
                                                            {/* Columnas para todas las enfermedades */}
                                                            <td style={{ fontSize: '1.05rem', textAlign: 'center' }}>
                                                                {registro.ejercicio ? <><strong style={{ color: '#10b981' }}>{registro.ejercicio}</strong> min</> : <span style={{ color: '#9ca3af' }}>-</span>}
                                                            </td>
                                                            <td style={{ fontSize: '1.05rem', textAlign: 'center' }}>
                                                                {registro.calorias ? <><strong style={{ color: '#f59e0b' }}>{registro.calorias}</strong> kcal</> : <span style={{ color: '#9ca3af' }}>-</span>}
                                                            </td>
                                                            <td style={{ fontSize: '0.95rem', textAlign: 'center' }}>
                                                                {registro.estadoAnimo ? (
                                                                    <span style={{
                                                                        padding: '0.25rem 0.75rem',
                                                                        borderRadius: '0.5rem',
                                                                        background: registro.estadoAnimo === 'Bueno' ? '#d1fae5' : registro.estadoAnimo === 'Malo' ? '#fee2e2' : '#fef3c7',
                                                                        color: registro.estadoAnimo === 'Bueno' ? '#065f46' : registro.estadoAnimo === 'Malo' ? '#991b1b' : '#92400e',
                                                                        fontWeight: 600
                                                                    }}>
                                                                        {registro.estadoAnimo === 'Bueno' ? 'Bueno' : registro.estadoAnimo === 'Malo' ? 'Malo' : 'Regular'}
                                                                    </span>
                                                                ) : <span style={{ color: '#9ca3af' }}>-</span>}
                                                            </td>
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <div className="collapsible-section">
                        <div className="section-header" onClick={() => setExpandedExercises(!expandedExercises)}>
                            <div className="section-title">
                                <Activity />
                                <h3>Ejercicios Realizados ({ejerciciosRealizados.length})</h3>
                            </div>
                            {expandedExercises ? <ChevronUp /> : <ChevronDown />}
                        </div>
                        {expandedExercises && (
                            <div className="section-content">
                                {ejerciciosRealizados.length === 0 ? (
                                    <p className="empty-message">No hay ejercicios realizados</p>
                                ) : (
                                    <div className="history-table">
                                        <table>
                                            <thead>
                                                <tr>
                                                    <th>#</th>
                                                    <th>Ejercicio</th>
                                                    <th>Fecha</th>
                                                    <th>Hora</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {ejerciciosRealizados.map((ejercicio, index) => {
                                                    const fecha = new Date(ejercicio.completadoEn);
                                                    return (
                                                        <tr key={index}>
                                                            <td style={{ fontWeight: 700, textAlign: 'center', background: 'var(--bg-secondary)' }}>{ejerciciosRealizados.length - index}</td>
                                                            <td style={{ fontSize: '1rem' }}>
                                                                <span style={{ color: enfermedadColor, fontWeight: 600 }}>
                                                                    {ejercicio.ejercicio.nombre}
                                                                </span>
                                                            </td>
                                                            <td style={{ fontWeight: 600 }}>{fecha.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                                                            <td style={{ fontSize: '0.875rem' }}>{fecha.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}</td>
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </PageTransition>
        </Layout>
    );
};

export default PatientProfile;
