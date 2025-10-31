#!/bin/bash
echo "Iniciando Cliente de Chat..."
cd bin
java ChatClient localhost:5000 localhost:5001 localhost:5002
