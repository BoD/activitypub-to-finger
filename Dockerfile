FROM ubuntu:20.04
LABEL maintainer="BoD <BoD@JRAF.org>"

# Install libcurl
RUN apt-get update && apt-get install -y libcurl4-openssl-dev

# Copy the binary
COPY app/build/bin/linuxX64/releaseExecutable/activitypub-to-finger.kexe activitypub-to-finger.kexe

EXPOSE 8042
EXPOSE 7900

ENTRYPOINT ["./activitypub-to-finger.kexe"]
