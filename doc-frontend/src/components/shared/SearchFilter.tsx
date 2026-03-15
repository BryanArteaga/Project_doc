import React from 'react';
import { Search, Filter, X } from 'lucide-react';
import './SearchFilter.css';

export interface SearchFilterProps {
  searchValue: string;
  onSearchChange: (value: string) => void;
  onSearchSubmit: () => void;
  filterOptions?: Array<{
    name: string;
    label: string;
    type: 'select' | 'text';
    options?: Array<{ label: string; value: string }>;
    value: string;
  }>;
  onFilterChange?: (filterName: string, value: string) => void;
  onApplyFilters?: () => void;
  onClearFilters?: () => void;
  isDarkMode?: boolean;
  placeholder?: string;
}

/**
 * SearchFilter - Componente reutilizable de búsqueda + filtros
 * Maneja: input de búsqueda, filtros dinámicos, aplicar/limpiar
 */
export const SearchFilter: React.FC<SearchFilterProps> = ({
  searchValue,
  onSearchChange,
  onSearchSubmit,
  filterOptions = [],
  onFilterChange,
  onApplyFilters,
  onClearFilters,
  isDarkMode = false,
  placeholder = 'Buscar...',
}) => {
  const [showFilters, setShowFilters] = React.useState(false);

  const handleSearchKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      onSearchSubmit();
    }
  };

  const containerClass = `search-filter-wrapper ${isDarkMode ? 'search-filter--dark' : ''}`;

  return (
    <div className={containerClass}>
      {/* Search Bar */}
      <div className="search-filter-bar">
        <Search size={20} />
        <input
          type="text"
          value={searchValue}
          onChange={(e) => onSearchChange(e.target.value)}
          onKeyPress={handleSearchKeyPress}
          placeholder={placeholder}
          className="search-filter-input"
        />
        <button
          type="button"
          onClick={onSearchSubmit}
          className="search-filter-btn-search"
        >
          Buscar
        </button>

        {/* Filter Toggle Button */}
        {filterOptions.length > 0 && (
          <button
            type="button"
            onClick={() => setShowFilters(!showFilters)}
            className="search-filter-btn-toggle"
            title={showFilters ? 'Cerrar filtros' : 'Abrir filtros'}
          >
            <Filter size={18} />
            {showFilters && <X size={16} />}
          </button>
        )}
      </div>

      {/* Filters Panel */}
      {showFilters && filterOptions.length > 0 && (
        <div className="search-filter-panel">
          {filterOptions.map((filter) => (
            <div key={filter.name} className="search-filter-group">
              <label className="search-filter-label">{filter.label}</label>
              {filter.type === 'select' ? (
                <select
                  value={filter.value}
                  onChange={(e) => onFilterChange?.(filter.name, e.target.value)}
                  className="search-filter-select"
                >
                  <option value="">Todos</option>
                  {filter.options?.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  type="text"
                  value={filter.value}
                  onChange={(e) => onFilterChange?.(filter.name, e.target.value)}
                  className="search-filter-input-small"
                />
              )}
            </div>
          ))}

          <div className="search-filter-actions">
            <button
              type="button"
              onClick={onApplyFilters}
              className="search-filter-btn-apply"
            >
              Aplicar
            </button>
            <button
              type="button"
              onClick={onClearFilters}
              className="search-filter-btn-clear"
            >
              Limpiar
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
