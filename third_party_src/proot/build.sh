TERMUX_PKG_HOMEPAGE=https://proot-me.github.io/
TERMUX_PKG_DESCRIPTION="Emulate chroot, bind mount and binfmt_misc for non-root users"
TERMUX_PKG_LICENSE="GPL-2.0"
TERMUX_PKG_MAINTAINER="Michal Bednarski @michalbednarski"
TERMUX_PKG_VERSION=5.1.107.102
TERMUX_PKG_SRCURL=https://github.com/termux/proot/archive/v${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=SKIP_CHECKSUM
TERMUX_PKG_DEPENDS="libandroid-shmem, libtalloc"
TERMUX_PKG_BUILD_IN_SRC=true

termux_step_make() {
	export CFLAGS="$CFLAGS -D_GNU_SOURCE -Wno-error"
	export LDFLAGS="$LDFLAGS -landroid-shmem -ltalloc"
	cd src
	make -j ${TERMUX_MAKE_PROCESSES} V=1 \
		PROOT_UNBUNDLE_LOADER=1 \
		LOADER_PATH="/data/data/com.termux/files/usr/libexec/proot/loader"
}

termux_step_make_install() {
	cd src
	install -Dm755 proot "$TERMUX_PREFIX/bin/proot"
	install -Dm755 loader "$TERMUX_PREFIX/libexec/proot/loader"
}
