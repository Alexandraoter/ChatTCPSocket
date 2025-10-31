#!/bin/bash
echo "Iniciando Servidor 3 en puerto 5002..."
cd bin
java ChatServer 5002 Server3 localhost:5000 localhost:5001 2>&1 | tee ../logs/server3.log
