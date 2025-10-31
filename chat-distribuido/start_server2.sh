#!/bin/bash
echo "Iniciando Servidor 2 en puerto 5001..."
cd bin
java ChatServer 5001 Server2 localhost:5000 localhost:5002 2>&1 | tee ../logs/server2.log
