#!/bin/bash

sudo mkdir /root/.bitcoin
sudo bash -c 'cat <<EOF > /root/.bitcoin/bitcoin.conf
# Maintain full transaction index, used in lookups by the getrawtransaction call
txindex=1
listen=1

# Run bitcoind in regtest mode
regtest=1

# Accept command line and JSON-RPC commands
server=1
walletbroadcast=1

# Tells bitcoind that the RPC API settings on the following lines apply to the regtest RPC API
[regtest]

# RPC API settings
rpcconnect=localhost
rpcport=9997
rpcuser=user
rpcpassword=WLMClI3cZ3ghE3diSTK-ENHSenP0bnthnbYmrAg7hcM
rpcallowip=0.0.0.0/0
rpcbind=0.0.0.0
EOF'
