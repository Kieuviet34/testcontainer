# executor/images/python/Dockerfile
FROM python:3.11-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
    ca-certificates \
    coreutils \
 && rm -rf /var/lib/apt/lists/*

# non-root user
RUN groupadd -r sandbox && useradd -r -g sandbox -u 1000 -m -d /home/sandbox sandbox \
 && mkdir -p /sandbox && chown sandbox:sandbox /sandbox

WORKDIR /sandbox
USER sandbox
ENTRYPOINT ["bash","-lc"]
