import React, { ReactNode, useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import './ExpandableSection.css';

export interface ExpandableSectionProps {
  title: string;
  icon?: ReactNode;
  isExpandedInitially?: boolean;
  isDarkMode?: boolean;
  children: ReactNode;
  className?: string;
}

/**
 * ExpandableSection - Sección expandible/colapsable genérica
 * Reutilizable en cualquier página que necesite secciones expandibles
 */
export const ExpandableSection: React.FC<ExpandableSectionProps> = ({
  title,
  icon,
  isExpandedInitially = false,
  isDarkMode = false,
  children,
  className = '',
}) => {
  const [isExpanded, setIsExpanded] = useState(isExpandedInitially);

  const containerClass = `expandable-section ${isDarkMode ? 'expandable-section--dark' : ''} ${className}`;

  return (
    <div className={containerClass}>
      <button
        className="expandable-header"
        onClick={() => setIsExpanded(!isExpanded)}
        type="button"
      >
        <div className="expandable-title">
          {icon && <span className="expandable-icon">{icon}</span>}
          <h2 className="expandable-heading">{title}</h2>
        </div>
        <div className="expandable-toggle">
          {isExpanded ? <ChevronUp size={24} /> : <ChevronDown size={24} />}
        </div>
      </button>

      {isExpanded && (
        <div className="expandable-content">
          {children}
        </div>
      )}
    </div>
  );
};
