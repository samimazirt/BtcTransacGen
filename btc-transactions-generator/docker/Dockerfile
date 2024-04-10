# Use an official Ubuntu as a parent image
FROM ubuntu:20.04

# Set environment variables to avoid user interaction during installation
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y wget

# Set Bitcoin Core version
ENV BITCOIN_VERSION=25.0
ENV BITCOIN_URL=https://bitcoincore.org/bin/bitcoin-core-$BITCOIN_VERSION/bitcoin-$BITCOIN_VERSION-x86_64-linux-gnu.tar.gz \
	BITCOIN_SHA256=33930d432593e49d58a9bff4c30078823e9af5d98594d2935862788ce8a20aec

# Download, verify, extract, and install Bitcoin Core
RUN wget ${BITCOIN_URL} -O bitcoin.tar.gz \
    && echo "${BITCOIN_SHA256} bitcoin.tar.gz" | sha256sum -c - \
    && tar -xzvf bitcoin.tar.gz -C /usr/local --strip-components=1 \
    && rm bitcoin.tar.gz

# Copy the Bitcoin configuration file into the container
COPY root/.bitcoin/bitcoin.conf /root/.bitcoin/bitcoin.conf

ADD data/ /data/

# Expose the Bitcoin daemon port
EXPOSE 18444

# Run bitcoind by default
#CMD ["bitcoind", "-regtest"]
