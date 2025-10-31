#!/bin/bash
echo "Deteniendo todos los procesos del chat..."
pkill -f "ChatServer"
pkill -f "ChatClient"
echo "Procesos detenidos."
