#!/bin/bash

# Script de configuración para el sistema de chat distribuido
# Para Ubuntu Linux

echo "=== Sistema de Chat Distribuido con Tolerancia a Fallos ==="
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar Java
echo -e "${YELLOW}Verificando Java...${NC}"
if command -v java &> /dev/null && command -v javac &> /dev/null
then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✓ Java encontrado: $JAVA_VERSION${NC}"
else
    echo -e "${RED}✗ Java no encontrado${NC}"
    echo "Instalando OpenJDK..."
    sudo apt update
    sudo apt install -y openjdk-17-jdk
fi

echo ""

# Crear directorios
echo -e "${YELLOW}Creando estructura de directorios...${NC}"
mkdir -p src
mkdir -p bin
mkdir -p logs
echo -e "${GREEN}✓ Directorios creados${NC}"

echo ""

# Compilar archivos
echo -e "${YELLOW}Compilando código fuente...${NC}"
if [ -f "ChatServer.java" ]; then
    javac -d bin ChatServer.java
    echo -e "${GREEN}✓ ChatServer compilado${NC}"
else
    echo -e "${RED}✗ ChatServer.java no encontrado${NC}"
fi

if [ -f "ChatClient.java" ]; then
    javac -d bin ChatClient.java
    echo -e "${GREEN}✓ ChatClient compilado${NC}"
else
    echo -e "${RED}✗ ChatClient.java no encontrado${NC}"
fi

echo ""
echo -e "${GREEN}=== Compilación completada ===${NC}"
echo ""

# Crear scripts de ejecución
echo -e "${YELLOW}Creando scripts de inicio...${NC}"

# Script para servidor 1
cat > start_server1.sh << 'EOF'
#!/bin/bash
echo "Iniciando Servidor 1 en puerto 5000..."
cd bin
java ChatServer 5000 Server1 localhost:5001 localhost:5002 2>&1 | tee ../logs/server1.log
EOF
chmod +x start_server1.sh

# Script para servidor 2
cat > start_server2.sh << 'EOF'
#!/bin/bash
echo "Iniciando Servidor 2 en puerto 5001..."
cd bin
java ChatServer 5001 Server2 localhost:5000 localhost:5002 2>&1 | tee ../logs/server2.log
EOF
chmod +x start_server2.sh

# Script para servidor 3
cat > start_server3.sh << 'EOF'
#!/bin/bash
echo "Iniciando Servidor 3 en puerto 5002..."
cd bin
java ChatServer 5002 Server3 localhost:5000 localhost:5001 2>&1 | tee ../logs/server3.log
EOF
chmod +x start_server3.sh

# Script para cliente
cat > start_client.sh << 'EOF'
#!/bin/bash
echo "Iniciando Cliente de Chat..."
cd bin
java ChatClient localhost:5000 localhost:5001 localhost:5002
EOF
chmod +x start_client.sh

# Script para iniciar todos los servidores
cat > start_all_servers.sh << 'EOF'
#!/bin/bash
echo "=== Iniciando todos los servidores ==="
echo ""
echo "Servidor 1 (puerto 5000)..."
gnome-terminal -- bash -c "./start_server1.sh; exec bash" 2>/dev/null || xterm -e "./start_server1.sh" 2>/dev/null || ./start_server1.sh &

sleep 2
echo "Servidor 2 (puerto 5001)..."
gnome-terminal -- bash -c "./start_server2.sh; exec bash" 2>/dev/null || xterm -e "./start_server2.sh" 2>/dev/null || ./start_server2.sh &

sleep 2
echo "Servidor 3 (puerto 5002)..."
gnome-terminal -- bash -c "./start_server3.sh; exec bash" 2>/dev/null || xterm -e "./start_server3.sh" 2>/dev/null || ./start_server3.sh &

echo ""
echo "Todos los servidores iniciados. Espera 5 segundos antes de iniciar clientes."
EOF
chmod +x start_all_servers.sh

# Script para detener todo
cat > stop_all.sh << 'EOF'
#!/bin/bash
echo "Deteniendo todos los procesos del chat..."
pkill -f "ChatServer"
pkill -f "ChatClient"
echo "Procesos detenidos."
EOF
chmod +x stop_all.sh

echo -e "${GREEN}✓ Scripts de inicio creados${NC}"
echo ""

# Mostrar instrucciones
cat << 'EOF'
╔════════════════════════════════════════════════════════════╗
║         SISTEMA DE CHAT DISTRIBUIDO - LISTO               ║
╚════════════════════════════════════════════════════════════

EOF

echo -e "${GREEN}✓ Configuración completada${NC}"
echo ""
echo -e "${YELLOW}Para comenzar, ejecuta: ./start_all_servers.sh${NC}"
