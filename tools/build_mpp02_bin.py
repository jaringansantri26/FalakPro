#!/usr/bin/env python3
"""
Build FalakPro-compatible ELP/MPP02 binary data from the raw text files.

Source data:
    C:\\eclipse\\ELPMPP02\\elp_main.long
    C:\\eclipse\\ELPMPP02\\elp_main.lat
    C:\\eclipse\\ELPMPP02\\elp_main.dist
    C:\\eclipse\\ELPMPP02\\elp_pert.*

Primary output:
    mpp02_core.bin

FalakPro's ElpDataProvider expects a flat little-endian double stream:
    longitude orders T0..T3, latitude orders T0..T2, distance orders T0..T3
Each term is six doubles:
    amplitude, phase0, phase1, phase2, phase3, phase4

The raw ELP/MPP02 files are evaluated with DE405/DE406 parameters from
ElpMpp02.pdf. Main-problem amplitudes A~ are computed from A, B1..B5 using
the theory's fA/fB factors. Perturbation Poisson terms are converted to the
same polynomial-argument form used by FalakPro.
"""

from __future__ import annotations

import argparse
import math
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence


ARCSEC_TO_RAD = math.pi / 648000.0
RAD_TO_ARCSEC = 648000.0 / math.pi

LON_COUNTS = (12337, 1199, 219, 2)
LAT_COUNTS = (7380, 516, 52)
DIST_COUNTS = (12819, 1165, 210, 2)


@dataclass(frozen=True)
class MainTerm:
    i: tuple[int, int, int, int]
    a: float
    b: tuple[float, float, float, float, float, float]


@dataclass(frozen=True)
class PertTerm:
    i: tuple[int, ...]
    a: float
    phi0: float


@dataclass(frozen=True)
class DeParams:
    dw1_0: float = -0.07008
    dw2_0: float = 0.20794
    dw3_0: float = -0.07215
    dw1_1: float = -0.35106
    dw2_1: float = 0.08017
    dw3_1: float = -0.04317
    dw1_2: float = -0.03743
    dgam: float = 0.00085
    de: float = -0.00006
    deart_0: float = -0.00033
    deart_1: float = 0.00732
    dperi: float = -0.00749
    dep: float = 0.00224
    dw1_3: float = -0.00018865
    dw1_4: float = -0.00001024
    dw2_2: float = 0.00470602
    dw2_3: float = -0.00025213
    dw3_2: float = -0.00261070
    dw3_3: float = -0.00010712


@dataclass(frozen=True)
class Factors:
    d_w2_1: float
    d_w3_1: float
    f_a: float
    f_b: tuple[float, float, float, float, float]


def parse_main(path: Path) -> list[MainTerm]:
    lines = useful_lines(path)
    expected = int(lines[0])
    terms: list[MainTerm] = []
    for line in lines[1:]:
        parts = line.split()
        if len(parts) != 11:
            raise ValueError(f"{path.name}: expected 11 columns, got {len(parts)} in {line!r}")
        terms.append(
            MainTerm(
                i=tuple(int(parts[k]) for k in range(4)),
                a=float(parts[4]),
                b=tuple(float(parts[k]) for k in range(5, 11)),
            )
        )
    if len(terms) != expected:
        raise ValueError(f"{path.name}: header says {expected}, parsed {len(terms)}")
    return terms


def parse_pert(path: Path) -> list[PertTerm]:
    lines = useful_lines(path)
    expected = int(lines[0])
    terms: list[PertTerm] = []
    for line in lines[1:]:
        parts = line.split()
        if len(parts) != 15:
            raise ValueError(f"{path.name}: expected 15 columns, got {len(parts)} in {line!r}")
        terms.append(
            PertTerm(
                i=tuple(int(parts[k]) for k in range(13)),
                a=float(parts[13]),
                phi0=float(parts[14]),
            )
        )
    if len(terms) != expected:
        raise ValueError(f"{path.name}: header says {expected}, parsed {len(terms)}")
    return terms


def useful_lines(path: Path) -> list[str]:
    return [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]


def setup_de405_factors(params: DeParams) -> Factors:
    b0 = (
        (0.311079095, -0.103837907),
        (-0.004482398, 0.000668287),
        (-0.001102485, -0.001298072),
        (0.001056062, -0.000178028),
        (0.000050928, -0.000037342),
    )
    m = 0.074801329
    alpha = 0.002571881
    w1_1 = 1732559343.73604 + params.dw1_1
    w2_1 = 14643420.3171 + params.dw2_1
    w3_1 = -6967919.5383 + params.dw3_1

    delta_nu = 0.55604 + params.dw1_1
    delta_gam = -0.08066 + params.dgam
    delta_e = 0.01789 + params.de
    delta_e0 = -0.12879 + params.dep
    delta_n0 = -0.0642 + params.deart_1

    sum25 = b0[0][0] + (2.0 * alpha / (3.0 * m)) * b0[4][0]
    sum35 = b0[0][1] + (2.0 * alpha / (3.0 * m)) * b0[4][1]

    dgam_rad = params.dgam * ARCSEC_TO_RAD
    de_rad = params.de * ARCSEC_TO_RAD
    dep_rad = params.dep * ARCSEC_TO_RAD

    d_w2_1 = (
        ((w2_1 / w1_1) - m) * sum25 * params.dw1_1
        + sum25 * params.deart_1
        + w1_1 * (b0[1][0] * dgam_rad + b0[2][0] * de_rad + b0[3][0] * dep_rad)
    )
    d_w3_1 = (
        ((w3_1 / w1_1) - m) * sum35 * params.dw1_1
        + sum35 * params.deart_1
        + w1_1 * (b0[1][1] * dgam_rad + b0[2][1] * de_rad + b0[3][1] * dep_rad)
    )

    f_a = 1.0 - (2.0 * delta_nu) / (3.0 * w1_1)
    f_b1 = (delta_n0 - m * delta_nu) / w1_1
    # ElpMpp02.pdf states that fB2, fB3, and fB4 are in radians,
    # while the fitted deltas in the DE405/DE406 table are arcseconds.
    f_b2 = delta_gam * ARCSEC_TO_RAD
    f_b3 = delta_e * ARCSEC_TO_RAD
    f_b4 = delta_e0 * ARCSEC_TO_RAD
    f_b5 = (2.0 * alpha / (3.0 * m * w1_1)) * (delta_n0 - m * delta_nu)
    return Factors(d_w2_1=d_w2_1, d_w3_1=d_w3_1, f_a=f_a, f_b=(f_b1, f_b2, f_b3, f_b4, f_b5))


def argument_polynomials(params: DeParams, factors: Factors) -> list[tuple[float, float, float, float, float]]:
    w1 = (
        218.3148724777778 * 3600.0 + params.dw1_0,
        1732559343.73604 + params.dw1_1,
        -6.8084 + params.dw1_2,
        0.006604 + params.dw1_3,
        -0.00003169 + params.dw1_4,
    )
    w2 = (
        83.3530741944444 * 3600.0 + params.dw2_0,
        14643420.3171 + params.dw2_1 + factors.d_w2_1,
        -38.2631 + params.dw2_2,
        -0.045047 + params.dw2_3,
        0.00021301,
    )
    w3 = (
        125.0444814444444 * 3600.0 + params.dw3_0,
        -6967919.5383 + params.dw3_1 + factors.d_w3_1,
        6.359 + params.dw3_2,
        0.007625 + params.dw3_3,
        -0.00003586,
    )
    ea = (
        100.4664499722222 * 3600.0 + params.deart_0,
        129597742.293 + params.deart_1,
        -0.0202,
        9e-4,
        1.5e-7,
    )
    peri = (
        102.9373481666667 * 3600.0 + params.dperi,
        1161.24342,
        0.529265,
        -1.1814e-4,
        1.1379e-5,
    )

    def add(a: Sequence[float], b: Sequence[float]) -> tuple[float, float, float, float, float]:
        return tuple(a[i] + b[i] for i in range(5))  # type: ignore[return-value]

    def sub(a: Sequence[float], b: Sequence[float]) -> tuple[float, float, float, float, float]:
        return tuple(a[i] - b[i] for i in range(5))  # type: ignore[return-value]

    def linear(c0_deg: float, c1: float) -> tuple[float, float, float, float, float]:
        return (c0_deg * 3600.0, c1, 0.0, 0.0, 0.0)

    args_arcsec = [
        add(sub(w1, ea), (648000.0, 0.0, 0.0, 0.0, 0.0)),  # D
        sub(w1, w3),  # F
        sub(w1, w2),  # l
        sub(ea, peri),  # l'
        linear(252.2503991388889, 538101628.6689),  # Me
        linear(181.9797883333333, 210664136.4578),  # Ve
        linear(100.4664499722222, 129597742.293),  # EM
        linear(355.4332961111111, 68905077.6594),  # Ma
        linear(34.3514845555556, 10925660.5734),  # Ju
        linear(50.0774744722222, 4399609.3363),  # Sa
        linear(314.0556509722222, 1542482.5785),  # Ur
        linear(304.3488800277778, 786547.897),  # Ne
        add(w1, (0.0, 5028.79695, 0.0, 0.0, 0.0)),  # zeta
    ]
    return [tuple(x * ARCSEC_TO_RAD for x in poly) for poly in args_arcsec]


FALAKPRO_PHASE_COMPATIBLE_DELTAS = (
    (
        3.14927210083741e-05,
        2.98152171999733e-05,
        2.81538091939596e-05,
        -4.13594550662124e-07,
        8.62977563103167e-06,
        -1.20213479266001e-05,
        -3.92941490574193e-07,
        1.83103364385647e-05,
        1.69646001537613e-07,
        -2.71738077937362e-08,
        -7.75181490880914e-05,
        4.07374875140763e-06,
        3.10997795266613e-05,
    ),
    (
        -1.39243395013251e-11,
        -7.57033940841909e-09,
        9.89581630407474e-09,
        -1.83971514454304e-12,
        -1.01860048926853e-10,
        -1.46953204298216e-10,
        -8.31572645507182e-13,
        -1.94259161662501e-10,
        -2.42759063171303e-10,
        9.68331386537905e-11,
        -2.42887943638819e-10,
        -1.09145143052167e-12,
        -1.52982380890959e-11,
    ),
    (0.0,) * 13,
    (
        4.31968989868593e-09,
        0.0,
        0.0,
        -4.31968989868586e-09,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
    ),
    (0.0,) * 13,
)


def apply_falakpro_phase_compatibility(
    arg_polys: Sequence[Sequence[float]],
) -> list[tuple[float, float, float, float, float]]:
    corrected = []
    for arg_index, poly in enumerate(arg_polys):
        corrected.append(
            tuple(poly[power] + FALAKPRO_PHASE_COMPATIBLE_DELTAS[power][arg_index] for power in range(5))
        )
    return corrected


def phase_from_indices(indices: Sequence[int], arg_polys: Sequence[Sequence[float]]) -> tuple[float, float, float, float, float]:
    phase = [0.0, 0.0, 0.0, 0.0, 0.0]
    for idx, poly in zip(indices, arg_polys):
        if idx:
            for power in range(5):
                phase[power] += idx * poly[power]
    return tuple(phase)  # type: ignore[return-value]


def falak_w_poly() -> tuple[float, float, float, float, float]:
    return (
        3.81034409083088,
        8399.68473007193,
        -0.0000331895204255009,
        3.11024944910606e-08,
        -2.03282376489228e-10,
    )


def poly_sub(a: Sequence[float], b: Sequence[float]) -> tuple[float, float, float, float, float]:
    return tuple(a[i] - b[i] for i in range(5))  # type: ignore[return-value]


def normalize_phase(term: tuple[float, float, float, float, float]) -> tuple[float, float, float, float, float]:
    phase0 = math.remainder(term[0], 2.0 * math.pi)
    return (phase0, term[1], term[2], term[3], term[4])


def main_to_flat_terms(
    terms: Iterable[MainTerm],
    arg_polys: Sequence[Sequence[float]],
    factors: Factors,
    kind: str,
) -> list[tuple[float, float, float, float, float, float]]:
    out = []
    w_poly = falak_w_poly()
    for term in terms:
        b1, b2, b3, b4, b5, _b6 = term.b
        amp = term.a + factors.f_b[0] * b1 + factors.f_b[1] * b2 + factors.f_b[2] * b3 + factors.f_b[3] * b4 + factors.f_b[4] * b5
        if kind == "dist":
            amp = factors.f_a * term.a + factors.f_b[0] * b1 + factors.f_b[1] * b2 + factors.f_b[2] * b3 + factors.f_b[3] * b4 + factors.f_b[4] * b5
        phase = phase_from_indices(term.i, arg_polys[:4])

        if kind == "long":
            amp *= RAD_TO_ARCSEC
        elif kind == "lat":
            amp *= RAD_TO_ARCSEC
        elif kind == "dist":
            # Main distance is a cosine series in the theory. FalakPro's flat
            # provider evaluates every term as amp * sin(poly), so encode cos(x)
            # as sin(x + pi/2).
            phase = (phase[0] + math.pi / 2.0, phase[1], phase[2], phase[3], phase[4])

        out.append((amp, *normalize_phase(phase)))
    return out


def pert_to_flat_terms(
    terms: Iterable[PertTerm],
    arg_polys: Sequence[Sequence[float]],
    kind: str,
) -> list[tuple[float, float, float, float, float, float]]:
    out = []
    for term in terms:
        phase = list(phase_from_indices(term.i, arg_polys))
        phase[0] += term.phi0
        amp = term.a
        if kind == "long":
            amp *= RAD_TO_ARCSEC
        elif kind == "lat":
            amp *= RAD_TO_ARCSEC
        out.append((amp, *normalize_phase(tuple(phase))))
    return out


def build_falak_core(raw_dir: Path, output: Path, falakpro_phase_compatible: bool = False) -> None:
    params = DeParams()
    factors = setup_de405_factors(params)
    arg_polys = argument_polynomials(params, factors)
    if falakpro_phase_compatible:
        arg_polys = apply_falakpro_phase_compatibility(arg_polys)

    lon = main_to_flat_terms(parse_main(raw_dir / "elp_main.long"), arg_polys, factors, "long")
    lat = main_to_flat_terms(parse_main(raw_dir / "elp_main.lat"), arg_polys, factors, "lat")
    dist = main_to_flat_terms(parse_main(raw_dir / "elp_main.dist"), arg_polys, factors, "dist")

    lon_orders = [
        lon + pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.longT0"), arg_polys, "long"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.longT1"), arg_polys, "long"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.longT2"), arg_polys, "long"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.longT3"), arg_polys, "long"),
    ]
    lat_orders = [
        lat + pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.latT0"), arg_polys, "lat"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.latT1"), arg_polys, "lat"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.latT2"), arg_polys, "lat"),
    ]
    dist_orders = [
        dist + pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.distT0"), arg_polys, "dist"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.distT1"), arg_polys, "dist"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.distT2"), arg_polys, "dist"),
        pert_to_flat_terms(parse_pert(raw_dir / "elp_pert.distT3"), arg_polys, "dist"),
    ]

    counts = tuple(len(x) for x in lon_orders), tuple(len(x) for x in lat_orders), tuple(len(x) for x in dist_orders)
    if counts != (LON_COUNTS, LAT_COUNTS, DIST_COUNTS):
        raise ValueError(f"Unexpected term counts: got {counts}, expected {(LON_COUNTS, LAT_COUNTS, DIST_COUNTS)}")

    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("wb") as f:
        for orders in (lon_orders, lat_orders, dist_orders):
            for order_terms in orders:
                for item in order_terms:
                    f.write(struct.pack("<6d", *item))


def write_de405_params(path: Path) -> None:
    p = DeParams()
    values = (
        p.dw1_0, p.dw2_0, p.dw3_0, p.dw1_1, p.dw2_1, p.dw3_1, p.dw1_2, p.dgam,
        p.de, p.deart_0, p.deart_1, p.dperi, p.dep, p.dw1_3, p.dw1_4, p.dw2_2,
        p.dw2_3, p.dw3_2, p.dw3_3,
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as f:
        f.write(struct.pack("<i", len(values)))
        f.write(struct.pack("<" + "d" * len(values), *values))


def main() -> None:
    parser = argparse.ArgumentParser(description="Build FalakPro mpp02_core.bin from raw ELP/MPP02 text data.")
    parser.add_argument("--raw-dir", default=r"C:\eclipse\ELPMPP02", type=Path)
    parser.add_argument("--output", default=r"C:\FalakPro\app\src\main\assets\mpp02_core_from_elpmp02_de405.bin", type=Path)
    parser.add_argument("--params-output", default=None, type=Path)
    parser.add_argument(
        "--falakpro-phase-compatible",
        action="store_true",
        help="Apply the small fundamental-argument phase deltas recovered from FalakPro's existing mpp02_core.bin.",
    )
    args = parser.parse_args()

    build_falak_core(args.raw_dir, args.output, falakpro_phase_compatible=args.falakpro_phase_compatible)
    print(f"Wrote {args.output} ({args.output.stat().st_size} bytes)")
    if args.params_output:
        write_de405_params(args.params_output)
        print(f"Wrote {args.params_output} ({args.params_output.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
