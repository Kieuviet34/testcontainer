# executor/images/gcc/Dockerfile
FROM debian:bookworm-slim

# 1. Cài đặt toolchain + coreutils (đã bao gồm stdbuf + timeout) + locales
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        coreutils \
        locales \
    && rm -rf /var/lib/apt/lists/*

# 2. Fix locale (bắt buộc để stdout không bị buffer + tránh perl warning)
RUN echo "en_US.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen en_US.UTF-8

ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8

# 3. Tạo user sandbox UID=1000 (giữ nguyên yêu cầu bảo mật)
RUN groupadd -r sandbox && \
    useradd -r -g sandbox -u 1000 -m -d /home/sandbox sandbox && \
    mkdir -p /sandbox && \
    chown sandbox:sandbox /sandbox

# 4. Chuyển sang non-root + working directory
WORKDIR /sandbox
USER sandbox

# 5. ÉP stdout/stderr line-buffered ngay từ ENTRYPOINT (fix 100% mất output)
ENTRYPOINT ["stdbuf", "-oL", "-eL", "bash", "-lc"]