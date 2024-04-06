# Application Setup and Execution Guide

This guide provides step-by-step instructions for setting up and running the Application in conjunction with Bitcoin Core in `regtest` mode.

## Setting Up Bitcoin Core

1. **Download and Install Bitcoin Core**

   Download the `bitcoind` and `bitcoin-cli` executables from the [official Bitcoin Core website](https://bitcoin.org/en/download) suitable for your operating system.

2. **Configure Bitcoin Core**

   Create a `bitcoin.conf` file in the root directory of your Bitcoin Core installation and populate it with the following settings:

   ```plaintext
   # Maintain a full transaction index, used by the getrawtransaction call
   txindex=1

   # Run bitcoind in regtest mode
   regtest=1

   # Accept command line and JSON-RPC commands
   server=1

   # RPC API settings for regtest mode
   [regtest]
   rpcconnect=localhost
   rpcport=9997
   rpcuser=
   rpcpassword=
   # Optional: rpcauth=
   ```

3. **Generate RPC Credentials**

   Generate new RPC credentials using the `rpcauth.py` script located in `<unpacked_folder>/share/rpcauth`. Replace `<YOUR_USERNAME>` with your desired username:

   ```shell
   <unpacked_folder>/share/rpcauth/rpcauth.py <YOUR_USERNAME>
   ```

   Note the output and use it to replace the placeholders in `bitcoin.conf` for `rpcuser`, `rpcpassword`, and optionally `rpcauth`.

4. **Start Bitcoin Core**

   Start `bitcoind` with the following command, adjusting the `-conf` flag to the path of your `bitcoin.conf` file:

   ```shell
   bitcoind -fallbackfee=0.0002 -conf=<Path/To/bitcoin.conf>
   ```

## Running the Application

### Compilation

Ensure Maven is installed on your system. Compile the project from the root directory using Maven:

```shell
mvn compile
```

### Execution

Execute the application with Maven, specifying the main class and the arguments for the output file name and the number of transactions:

```shell
mvn exec:java -Dexec.mainClass="com.BitcoinTransacGen.TransactionsGenPlugin" -Dexec.args="-o test.arff -n 3"
```

### Packaging for WEKA

To create a package for integration with WEKA:

1. **Package the Application**

   Clean and package the project with Maven:

   ```shell
   mvn clean package
   ```

   This command generates a `.zip` file in the project's `dist` folder.

2. **Import the Package into WEKA**

   - Open WEKA, navigate to `Tools > Package Manager > Unofficial > File/URL`.
   - Select the `.zip` file created by the previous step, import it, and restart WEKA for the changes to take effect.

### Usage in WEKA

- With WEKA opened, navigate to Explorer or Workbench.
- Click on `Generate`.
- Select `BtcTransacGen`, adjust the number of transactions as desired, and click `Generate`.
- **Note**: Make sure the `bitcoind` server is running before attempting to generate transactions.

### Troubleshooting

If you encounter any issues, consider deleting the `regtest` folder within your Bitcoin Core data directory (`AppData/Roaming/Bitcoin` on Windows, or the corresponding directory on Unix systems), then restart the `bitcoind` server and try regenerating the transactions.
