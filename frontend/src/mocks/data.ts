export const dashboardStats = [
  { label: 'Órdenes abiertas', value: '24', change: '+12%', tone: 'blue' },
  { label: 'En diagnóstico', value: '08', change: '+4%', tone: 'orange' },
  { label: 'Listas para retirar', value: '06', change: '+8%', tone: 'green' },
  { label: 'Stock bajo', value: '05', change: 'Revisar', tone: 'red' },
];

export const clients = [
  { name: 'María González', contact: 'maria.gonzalez@email.com', equipment: '2 equipos', status: 'Activo' },
  { name: 'Carlos Rodríguez', contact: '11 4567-8899', equipment: '1 equipo', status: 'Activo' },
  { name: 'Lucía Fernández', contact: 'lucia.fernandez@email.com', equipment: '3 equipos', status: 'Activo' },
  { name: 'Pablo Martínez', contact: 'pablo.martinez@email.com', equipment: '1 equipo', status: 'Inactivo' },
];

export const orders = [
  { id: '#OT-1042', client: 'María González', equipment: 'Notebook Lenovo IdeaPad', state: 'EN_REPARACION', date: '06/11/2026' },
  { id: '#OT-1041', client: 'Carlos Rodríguez', equipment: 'iPhone 13', state: 'PENDIENTE_APROBACION', date: '05/11/2026' },
  { id: '#OT-1040', client: 'Lucía Fernández', equipment: 'PC de escritorio', state: 'LISTA_PARA_RETIRAR', date: '04/11/2026' },
  { id: '#OT-1039', client: 'Pablo Martínez', equipment: 'Monitor Samsung', state: 'RECIBIDA', date: '04/11/2026' },
];

export const spareParts = [
  { code: 'REP-001', name: 'Batería Lenovo L14', category: 'Baterías', stock: 2, minimum: 5, price: '$ 45.000' },
  { code: 'REP-002', name: 'Pantalla iPhone 13', category: 'Pantallas', stock: 8, minimum: 3, price: '$ 120.000' },
  { code: 'REP-003', name: 'SSD 480GB Kingston', category: 'Almacenamiento', stock: 1, minimum: 4, price: '$ 38.500' },
  { code: 'REP-004', name: 'Memoria RAM 8GB DDR4', category: 'Memorias', stock: 12, minimum: 5, price: '$ 29.900' },
];

export const users = [
  { name: 'Admin FixHub', contact: 'admin@fixhub.com', roles: 'ADMINISTRADOR', status: 'Activo' },
  { name: 'Sofía Técnico', contact: 'sofia@fixhub.com', roles: 'TECNICO', status: 'Activo' },
  { name: 'Recepción', contact: 'recepcion@fixhub.com', roles: 'RECEPCIONISTA', status: 'Activo' },
];
