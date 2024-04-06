SHAREPOINT: https://epitafr-my.sharepoint.com/:f:/g/personal/sami_mazirt_epita_fr/EsYjAgviGqtDre3e60KQ43YBDUjcxrIrJCpQZsS9sKOq6g?e=2LkcnF

To run Application:

Download bitcoin core exe
add a bitcoin.conf file at the root of <user>
"""

# Maintain full transaction index, used in lookups by the getrawtransaction call
txindex=1

# Run bitcoind in regtest mode
regtest=1

# Accept command line and JSON-RPC commands
server=1

# Tells bitcoind that the RPC API settings on the following lines apply to the regtest RPC API
[regtest]

# RPC API settings
rpcconnect=localhost
rpcport=9997
rpcuser=<user>
rpcpassword=<password>
#rpcauth=<value>

""

Where you installed bitcoin core there should be a path: <unpacked_folder>/share/rpcauth
Run: <unpacked_folder>/share/rpcauth/rpcauth.py <YOUR_USERNAME>
Keep the output

replace rpcuser, rpcpassword and rpcauth with the output values of the previous step.

Run: bitcoind -fallbackfee=0.0002 -conf=<Path/To/bitcoin.conf>

Then run application.java.


need to run from command line:

mvn compile (you need maven)
mvn exec:java -Dexec.mainClass="com.BitcoinTransacGen.TransactionsGenPlugin" -Dexec.args="-o test.arff -n 3"
output name of the output file
n number of transactions

To make package for weka: run mvn clean package
Open weka, Tools, package manager, Unofficial, file/url, and select the zip file created by the previous command, should be in dist folder in the project, import and restart weka.

To use: launch explorer or workbench, click generate, select BtcTransacGen, modify the parameter number of transactions and click generate (for now make sure the bitcoind server is running)
If problem: delete the regtest folder in AppData/Roaming/Bitcoin (same for unix find the path and delete)
relaunch the bitcoind server
re generate
