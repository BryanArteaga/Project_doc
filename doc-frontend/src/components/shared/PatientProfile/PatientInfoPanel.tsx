import React from 'react';
import './PatientInfoPanel.css';

export interface PatientInfo {
  nombre: string;
  apellido: string;
  cedula?: string;
  edad?: number;
  sexo?: string;
  peso?: number;
  altura?: number;
  enfermedad?: string;
  enfermedad_color?: string;
  registradoEn?: string;
  ultimaSincronizacion?: string;
}

interface PatientInfoPanelProps {
  patient: PatientInfo;
  isDarkMode?: boolean;
}

/**
 * PatientInfoPanel - Panel con información básica del paciente
 * Muestra: nombre, cédula, edad, sexo, antropometría, enfermedad
 */
export const PatientInfoPanel: React.FC<PatientInfoPanelProps> = ({
  patient,
  isDarkMode = false,
}) => {
  const containerClass = `patient-info-panel ${isDarkMode ? 'patient-info-panel--dark' : ''}`;
  
  const displayPeso = patient.peso ? `${patient.peso} kg` : '-';
  const displayAltura = patient.altura ? `${patient.altura} m` : '-';
  const displaySexo = patient.sexo || '-';
  
  // Calcular IMC si hay peso y altura
  const imc = patient.peso && patient.altura 
    ? (patient.peso / (patient.altura ** 2)).toFixed(1)
    : null;

  return (
    <div className={containerClass}>
      {/* Header con nombre y enfermedad */}
      <div className="patient-info-header">
        <div className="patient-avatar" style={{
          background: patient.enfermedad_color || '#6b7280'
        }}>
          {patient.nombre.charAt(0)}{patient.apellido.charAt(0)}
        </div>
        <div className="patient-header-content">
          <h2 className="patient-name">{patient.nombre} {patient.apellido}</h2>
          {patient.cedula && (
            <p className="patient-cedula">{patient.cedula}</p>
          )}
          {patient.enfermedad && (
            <span 
              className="patient-enfermedad-badge"
              style={{ backgroundColor: patient.enfermedad_color || '#6b7280' }}
            >
              {patient.enfermedad}
            </span>
          )}
        </div>
      </div>

      {/* Grid de información */}
      <div className="patient-info-grid">
        {patient.edad && (
          <div className="info-card">
            <span className="info-label">Edad</span>
            <span className="info-value">{patient.edad} años</span>
          </div>
        )}
        
        {patient.sexo && (
          <div className="info-card">
            <span className="info-label">Sexo</span>
            <span className="info-value">{displaySexo}</span>
          </div>
        )}
        
        {patient.peso && (
          <div className="info-card">
            <span className="info-label">Peso</span>
            <span className="info-value">{displayPeso}</span>
          </div>
        )}
        
        {patient.altura && (
          <div className="info-card">
            <span className="info-label">Altura</span>
            <span className="info-value">{displayAltura}</span>
          </div>
        )}
        
        {imc && (
          <div className="info-card">
            <span className="info-label">IMC</span>
            <span className="info-value">{imc}</span>
          </div>
        )}
        
        {patient.registradoEn && (
          <div className="info-card">
            <span className="info-label">Registrado</span>
            <span className="info-value">
              {new Date(patient.registradoEn).toLocaleDateString()}
            </span>
          </div>
        )}
      </div>
    </div>
  );
};
