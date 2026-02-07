// Helper para castear parámetros de ruta a string
export const getParamAsString = (param: string | string[] | undefined): string => {
  if (Array.isArray(param)) {
    return param[0] || '';
  }
  return param || '';
};

// Helper para convertir param a número
export const getParamAsNumber = (param: string | string[] | undefined): number => {
  const str = getParamAsString(param);
  return parseInt(str, 10);
};
