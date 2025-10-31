#!/bin/bash
echo "Iniciando Servidor 1 en puerto 5000..."
cd bin
java ChatServer 5000 Server1 localhost:5001 localhost:5002 2>&1 | tee ../logs/server1.log
