import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../../components/common/Layout/Layout';
import Loading from '../../components/common/Loading';
import PageTransition from '../../components/common/PageTransition';
import { ArrowLeft, User, Activity, TrendingUp, Calendar, ChevronDown, ChevronUp, ArrowUp, ArrowDown } from 'lucide-react';
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
    registradoEn: string;
    ultimaSincronizacion?: string;
    registrosSalud: Array<{
        id: number;
        presionSistolica?: number;
        presionDiastolica?: number;
        glucosa?: number;
        peso?: number;
        ejercicioMinutos?: number;  // ✅ Minutos de ejercicio
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
    const [expandedHealth, setExpandedHealth] = useState(false);
    const [expandedExercises, setExpandedExercises] = useState(false);
    const [sortColumn, setSortColumn] = useState<string>('');
    const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

    // Cargar datos inicialmente
    useEffect(() => {
        loadPatientData();
    }, [id]);

    // ⏱️ NUEVO: Real-time polling cada 5 segundos para ver cambios del paciente en tiempo real
    useEffect(() => {
        if (!id) return;

        const pollInterval = setInterval(() => {
            loadPatientDataSilent();
        }, 5000); // Polling cada 5 segundos

        return () => clearInterval(pollInterval);
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

    // 🔄 Versión silenciosa del loading para el polling (sin mostrar loader)
    const loadPatientDataSilent = async () => {
        try {
            const response = await api.get(`/api/doctor/patients/${id}`);
            setData(response.data);
        } catch (error: any) {
            // No mostrar error en polling automático, solo en console
            console.warn('Error en polling automático:', error.message);
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

    const latestHealth = registrosSalud[0];

    // Color por enfermedad (coinciden con la app Android)
    const enfermedadColor = patient.enfermedad === 'Hipertensión' ? '#D32F2F' :
        patient.enfermedad === 'Diabetes' ? '#FFF176' :
            patient.enfermedad === 'Obesidad' ? '#81D4FA' :
                patient.enfermedad === 'Colesterol alto' ? '#FFA726' : '#6b7280';

    // Función para ordenar registros
    const handleSort = (column: string) => {
        if (sortColumn === column) {
            setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
        } else {
            setSortColumn(column);
            setSortOrder('asc');
        }
    };

    // Ordenar registros según columna seleccionada
    const sortedRegistros = [...registrosSalud].sort((a, b) => {
        if (!sortColumn) return 0;
        
        let aValue: any = 0;
        let bValue: any = 0;

        switch (sortColumn) {
            case 'id':
                aValue = a.id;
                bValue = b.id;
                break;
            case 'fecha':
                aValue = new Date(a.registradoEn).getTime();
                bValue = new Date(b.registradoEn).getTime();
                break;
            case 'hora':
                const aFechaHora = new Date(a.registradoEn);
                const bFechaHora = new Date(b.registradoEn);
                aValue = aFechaHora.getHours() * 60 + aFechaHora.getMinutes();
                bValue = bFechaHora.getHours() * 60 + bFechaHora.getMinutes();
                break;
            case 'presion':
                aValue = (a.presionSistolica || 0) + (a.presionDiastolica || 0);
                bValue = (b.presionSistolica || 0) + (b.presionDiastolica || 0);
                break;
            case 'glucosa':
                aValue = a.glucosa || 0;
                bValue = b.glucosa || 0;
                break;
            case 'peso':
                aValue = a.peso || 0;
                bValue = b.peso || 0;
                break;
            case 'ejercicio':
                aValue = a.ejercicio || 0;
                bValue = b.ejercicio || 0;
                break;
            case 'calorias':
                aValue = a.calorias || 0;
                bValue = b.calorias || 0;
                break;
            case 'estadoAnimo':
                const orden = { 'Bueno': 3, 'Regular': 2, 'Malo': 1 };
                aValue = orden[a.estadoAnimo as keyof typeof orden] || 0;
                bValue = orden[b.estadoAnimo as keyof typeof orden] || 0;
                break;
        }

        if (sortOrder === 'asc') {
            if (aValue < bValue) return -1;
            if (aValue > bValue) return 1;
            return 0;
        } else {
            if (aValue < bValue) return 1;
            if (aValue > bValue) return -1;
            return 0;
        }
    });

    // Ordenar ejercicios realizados
    const sortedEjercicios = [...ejerciciosRealizados].sort((a, b) => {
        if (!sortColumn) return 0;
        
        let aValue: any = 0;
        let bValue: any = 0;

        switch (sortColumn) {
            case 'ejercicio-id':
                aValue = a.id;
                bValue = b.id;
                break;
            case 'ejercicio-nombre':
                aValue = a.ejercicio.nombre.toLowerCase();
                bValue = b.ejercicio.nombre.toLowerCase();
                break;
            case 'ejercicio-fecha':
                aValue = new Date(a.completadoEn).getTime();
                bValue = new Date(b.completadoEn).getTime();
                break;
            case 'ejercicio-hora':
                const aEjercicioFecha = new Date(a.completadoEn);
                const bEjercicioFecha = new Date(b.completadoEn);
                aValue = aEjercicioFecha.getHours() * 60 + aEjercicioFecha.getMinutes();
                bValue = bEjercicioFecha.getHours() * 60 + bEjercicioFecha.getMinutes();
                break;
        }

        if (sortOrder === 'asc') {
            if (aValue < bValue) return -1;
            if (aValue > bValue) return 1;
            return 0;
        } else {
            if (aValue < bValue) return 1;
            if (aValue > bValue) return -1;
            return 0;
        }
    });

    // ✅ Función para formatear minutos de ejercicio
    const formatearMinutos = (minutos: number | undefined | null): string => {
        const totalMinutos = minutos ?? 0;
        
        if (totalMinutos <= 0) return '0 min';
        if (totalMinutos < 60) return `${totalMinutos} min`;
        
        const horas = Math.floor(totalMinutos / 60);
        const mins = totalMinutos % 60;
        
        if (mins > 0) {
            return `${horas}h ${mins}min`;
        } else {
            return `${horas}h`;
        }
    };

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

                    {/* Grid: Información Personal (izquierda) + Columna Derecha (Último Registro + Estadísticas) */}
                    <div className="profile-grid-custom">
                        {/* Columna Izquierda: Información Personal */}
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

                        {/* Columna Derecha: Stack de Último Registro + Estadísticas */}
                        <div className="right-column">
                            {/* Último Registro de Salud */}
                            {latestHealth ? (
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
                            ) : (
                                <div className="health-card">
                                    <div className="empty-state">
                                        <p>No hay registros</p>
                                    </div>
                                </div>
                            )}

                            {/* Estadísticas Clínicas - Siempre visible */}
                            {registrosSalud.length > 0 ? (() => {
                                const presiones = registrosSalud.filter(r => r.presionSistolica && r.presionDiastolica);
                                const glucosas = registrosSalud.filter(r => r.glucosa);
                                const pesos = registrosSalud.filter(r => r.peso);
                                const ejercicios = registrosSalud.filter(r => r.ejercicioMinutos && r.ejercicioMinutos > 0);  // ✅ Filtrar por ejercicioMinutos
                                const calorias = registrosSalud.filter(r => r.calorias);

                                const calcularPromedio = (arr: number[]) => arr.length > 0 ? Math.round(arr.reduce((a, b) => a + b, 0) / arr.length) : 0;
                                
                                // ✅ Calcular promedio diario de ejercicios (agrupando por fecha)
                                const calcularPromedioDiario = (registros: typeof ejercicios) => {
                                    if (registros.length === 0) return 0;
                                    
                                    // Agrupar por fecha
                                    const porDia = new Map<string, number>();
                                    registros.forEach(r => {
                                        const fecha = r.registradoEn ? new Date(r.registradoEn).toISOString().split('T')[0] : '';
                                        if (fecha) {
                                            porDia.set(fecha, (porDia.get(fecha) || 0) + (r.ejercicioMinutos || 0));
                                        }
                                    });
                                    
                                    // Promedio = suma total / número de días
                                    const totalMinutos = Array.from(porDia.values()).reduce((a, b) => a + b, 0);
                                    return porDia.size > 0 ? Math.round(totalMinutos / porDia.size) : 0;
                                };

                                return (
                                    <div className="stats-section">
                                        <div className="section-header-static">
                                            <TrendingUp />
                                            <h3>Estadísticas Clínicas</h3>
                                        </div>
                                        <div className="clinical-stats-horizontal">
                                    {patient.enfermedad === 'Hipertensión' && presiones.length > 0 && (
                                        <div className="stat-compact">
                                            <div className="stat-compact-title">Presión Arterial</div>
                                            <div className="stat-compact-row">
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Promedio</div>
                                                    <div className="stat-compact-value">{calcularPromedio(presiones.map(r => r.presionSistolica!))}/{calcularPromedio(presiones.map(r => r.presionDiastolica!))}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Mínima</div>
                                                    <div className="stat-compact-value">{Math.min(...presiones.map(r => r.presionSistolica!))}/{Math.min(...presiones.map(r => r.presionDiastolica!))}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Máxima</div>
                                                    <div className="stat-compact-value">{Math.max(...presiones.map(r => r.presionSistolica!))}/{Math.max(...presiones.map(r => r.presionDiastolica!))}</div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                    {patient.enfermedad === 'Diabetes' && glucosas.length > 0 && (
                                        <div className="stat-compact">
                                            <div className="stat-compact-title">Glucosa (mg/dL)</div>
                                            <div className="stat-compact-row">
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Promedio</div>
                                                    <div className="stat-compact-value">{calcularPromedio(glucosas.map(r => r.glucosa!))}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Mínima</div>
                                                    <div className="stat-compact-value">{Math.min(...glucosas.map(r => r.glucosa!))}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Máxima</div>
                                                    <div className="stat-compact-value">{Math.max(...glucosas.map(r => r.glucosa!))}</div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                    {patient.enfermedad === 'Obesidad' && pesos.length > 0 && (
                                        <div className="stat-compact">
                                            <div className="stat-compact-title">Peso (kg)</div>
                                            <div className="stat-compact-row">
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Promedio</div>
                                                    <div className="stat-compact-value">{(pesos.reduce((sum, r) => sum + r.peso!, 0) / pesos.length).toFixed(1)}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Mínimo</div>
                                                    <div className="stat-compact-value">{Math.min(...pesos.map(r => r.peso!))}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Máximo</div>
                                                    <div className="stat-compact-value">{Math.max(...pesos.map(r => r.peso!))}</div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                    {ejercicios.length > 0 && (
                                        <div className="stat-compact">
                                            <div className="stat-compact-title">Actividad Física</div>
                                            <div className="stat-compact-row">
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Total Minutos</div>
                                                    <div className="stat-compact-value">{formatearMinutos(ejercicios.reduce((sum, r) => sum + (r.ejercicioMinutos || 0), 0))}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Promedio/día</div>
                                                    <div className="stat-compact-value">{formatearMinutos(calcularPromedioDiario(ejercicios))}</div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                    {calorias.length > 0 && (
                                        <div className="stat-compact">
                                            <div className="stat-compact-title">Calorías</div>
                                            <div className="stat-compact-row">
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Total</div>
                                                    <div className="stat-compact-value">{calorias.reduce((sum, r) => sum + r.calorias!, 0)}</div>
                                                </div>
                                                <div className="stat-compact-item">
                                                    <div className="stat-compact-label">Promedio/día</div>
                                                    <div className="stat-compact-value">{calcularPromedioDiario(calorias as typeof ejercicios)}</div>
                                                </div>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })() : (
                        <div className="stats-section">
                            <div className="section-header-static">
                                <TrendingUp />
                                <h3>Estadísticas Clínicas</h3>
                            </div>
                            <div className="empty-state">
                                <p>No hay estadísticas en este momento</p>
                            </div>
                        </div>
                    )}
                        </div>
                    </div>

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
                                                    <th onClick={() => handleSort('id')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            #
                                                            {sortColumn === 'id' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('fecha')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Fecha
                                                            {sortColumn === 'fecha' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('hora')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Hora
                                                            {sortColumn === 'hora' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    {/* Hipertensión - solo presión */}
                                                    {patient.enfermedad === 'Hipertensión' && (
                                                        <th onClick={() => handleSort('presion')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                                Presión Arterial
                                                                {sortColumn === 'presion' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                            </div>
                                                        </th>
                                                    )}
                                                    {/* Diabetes - solo glucosa */}
                                                    {patient.enfermedad === 'Diabetes' && (
                                                        <th onClick={() => handleSort('glucosa')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                                Glucosa (mg/dL)
                                                                {sortColumn === 'glucosa' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                            </div>
                                                        </th>
                                                    )}
                                                    {/* Obesidad - peso y presión */}
                                                    {patient.enfermedad === 'Obesidad' && (
                                                        <>
                                                            <th onClick={() => handleSort('peso')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                                    Peso (kg)
                                                                    {sortColumn === 'peso' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                                </div>
                                                            </th>
                                                            <th onClick={() => handleSort('presion')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                                    Presión Arterial
                                                                    {sortColumn === 'presion' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                                </div>
                                                            </th>
                                                        </>
                                                    )}
                                                    {/* Colesterol alto - glucosa y presión */}
                                                    {patient.enfermedad === 'Colesterol alto' && (
                                                        <>
                                                            <th onClick={() => handleSort('glucosa')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                                    Glucosa (mg/dL)
                                                                    {sortColumn === 'glucosa' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                                </div>
                                                            </th>
                                                            <th onClick={() => handleSort('presion')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                                    Presión Arterial
                                                                    {sortColumn === 'presion' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                                </div>
                                                            </th>
                                                        </>
                                                    )}
                                                    {/* Columnas para todas las enfermedades */}
                                                    <th onClick={() => handleSort('ejercicio')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Min. Ejercicio
                                                            {sortColumn === 'ejercicio' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('calorias')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Calorías
                                                            {sortColumn === 'calorias' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('estadoAnimo')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Estado de Ánimo
                                                            {sortColumn === 'estadoAnimo' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {sortedRegistros.map((registro, index) => {
                                                    const fecha = new Date(registro.registradoEn);
                                                    return (
                                                        <tr key={registro.id}>
                                                            <td style={{ fontWeight: 700, textAlign: 'center', background: 'var(--bg-secondary)' }}>
                                                                {index + 1}
                                                            </td>
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
                                                                <strong style={{ color: '#10b981' }}>{formatearMinutos(registro.ejercicioMinutos)}</strong>
                                                            </td>
                                                            <td style={{ fontSize: '1.05rem', textAlign: 'center' }}>
                                                                {registro.calorias !== undefined && registro.calorias !== null ? <><strong style={{ color: '#f59e0b' }}>{registro.calorias}</strong> kcal</> : <span style={{ color: '#9ca3af' }}>-</span>}
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
                                                    <th onClick={() => handleSort('ejercicio-id')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            #
                                                            {sortColumn === 'ejercicio-id' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('ejercicio-nombre')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Ejercicio
                                                            {sortColumn === 'ejercicio-nombre' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('ejercicio-fecha')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Fecha
                                                            {sortColumn === 'ejercicio-fecha' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                    <th onClick={() => handleSort('ejercicio-hora')} style={{ cursor: 'pointer', userSelect: 'none' }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'space-between' }}>
                                                            Hora
                                                            {sortColumn === 'ejercicio-hora' && (sortOrder === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />)}
                                                        </div>
                                                    </th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {sortedEjercicios.map((ejercicio, index) => {
                                                    const fecha = new Date(ejercicio.completadoEn);
                                                    return (
                                                        <tr key={ejercicio.id}>
                                                            <td style={{ fontWeight: 700, textAlign: 'center', background: 'var(--bg-secondary)' }}>
                                                                {index + 1}
                                                            </td>
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
