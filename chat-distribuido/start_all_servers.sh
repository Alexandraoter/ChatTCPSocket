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
