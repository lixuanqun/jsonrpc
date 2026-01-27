#!/usr/bin/env python3
import argparse
import asyncio
import json
import math
import statistics
import sys
import time
import uuid


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="JSON-RPC TCP load test (newline-delimited JSON)."
    )
    parser.add_argument("--host", default="127.0.0.1", help="Server host")
    parser.add_argument("--port", type=int, default=18080, help="Server port")
    parser.add_argument("--method", default="hello", help="RPC method name")
    parser.add_argument(
        "--params",
        default='["world"]',
        help="JSON params (array/object/null). Example: '[1,2]' or '{\"a\":1}'",
    )
    parser.add_argument(
        "--connections", type=int, default=10, help="Concurrent connections"
    )
    parser.add_argument(
        "--requests-per-connection",
        type=int,
        default=100,
        help="Requests per connection",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=5.0,
        help="Response timeout (seconds)",
    )
    parser.add_argument(
        "--notification",
        action="store_true",
        help="Send notifications (omit id, no response expected)",
    )
    parser.add_argument(
        "--expect-response",
        action="store_true",
        help="Wait for responses even if notification is set",
    )
    parser.add_argument(
        "--print-errors",
        action="store_true",
        help="Print error responses to stderr",
    )
    return parser.parse_args()


def parse_params(params_text: str):
    try:
        return json.loads(params_text)
    except json.JSONDecodeError as exc:
        raise ValueError(f"Invalid JSON for --params: {exc}") from exc


def percentile(sorted_values, p: float):
    if not sorted_values:
        return None
    if p <= 0:
        return sorted_values[0]
    if p >= 1:
        return sorted_values[-1]
    k = (len(sorted_values) - 1) * p
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return sorted_values[f]
    return sorted_values[f] + (sorted_values[c] - sorted_values[f]) * (k - f)


def build_request(method: str, params, request_id):
    req = {"jsonrpc": "2.0", "method": method}
    if params is not None:
        req["params"] = params
    if request_id is not None:
        req["id"] = request_id
    return req


async def run_connection(
    conn_id: int,
    host: str,
    port: int,
    method: str,
    params,
    requests_per_connection: int,
    timeout: float,
    expect_response: bool,
    print_errors: bool,
):
    reader, writer = await asyncio.open_connection(host, port)
    latencies = []
    sent = 0
    ok = 0
    errors = 0

    try:
        for _ in range(requests_per_connection):
            request_id = None if not expect_response else f"{conn_id}-{uuid.uuid4().hex}"
            req = build_request(method, params, request_id)
            payload = json.dumps(req, separators=(",", ":"), ensure_ascii=True) + "\n"
            start = time.perf_counter()
            writer.write(payload.encode("utf-8"))
            await writer.drain()
            sent += 1

            if not expect_response:
                ok += 1
                continue

            try:
                line = await asyncio.wait_for(reader.readline(), timeout=timeout)
            except asyncio.TimeoutError:
                errors += 1
                continue

            if not line:
                errors += 1
                break

            elapsed = time.perf_counter() - start
            latencies.append(elapsed)
            try:
                response = json.loads(line.decode("utf-8"))
            except json.JSONDecodeError:
                errors += 1
                if print_errors:
                    sys.stderr.write(
                        f"[conn {conn_id}] Invalid JSON response: {line!r}\n"
                    )
                continue

            if isinstance(response, dict) and response.get("error") is not None:
                errors += 1
                if print_errors:
                    sys.stderr.write(
                        f"[conn {conn_id}] RPC error response: {response}\n"
                    )
            else:
                ok += 1
    finally:
        writer.close()
        await writer.wait_closed()

    return {
        "sent": sent,
        "ok": ok,
        "errors": errors,
        "latencies": latencies,
    }


async def main_async(args: argparse.Namespace) -> int:
    try:
        params = parse_params(args.params)
    except ValueError as exc:
        sys.stderr.write(f"{exc}\n")
        return 2

    expect_response = args.expect_response or not args.notification

    start = time.perf_counter()
    tasks = []
    for i in range(args.connections):
        tasks.append(
            run_connection(
                conn_id=i,
                host=args.host,
                port=args.port,
                method=args.method,
                params=params,
                requests_per_connection=args.requests_per_connection,
                timeout=args.timeout,
                expect_response=expect_response,
                print_errors=args.print_errors,
            )
        )

    results = await asyncio.gather(*tasks, return_exceptions=False)
    elapsed = time.perf_counter() - start

    total_sent = sum(r["sent"] for r in results)
    total_ok = sum(r["ok"] for r in results)
    total_errors = sum(r["errors"] for r in results)
    latencies = [lat for r in results for lat in r["latencies"]]

    print("=== JSON-RPC Load Test Summary ===")
    print(f"Target: {args.host}:{args.port}  Method: {args.method}")
    print(f"Connections: {args.connections}")
    print(f"Requests per connection: {args.requests_per_connection}")
    print(f"Notification: {args.notification}  Expect response: {expect_response}")
    print(f"Elapsed: {elapsed:.3f}s")
    print(f"Sent: {total_sent}  OK: {total_ok}  Errors: {total_errors}")

    if latencies:
        latencies_sorted = sorted(latencies)
        avg = statistics.mean(latencies_sorted)
        p50 = statistics.median(latencies_sorted)
        p95 = percentile(latencies_sorted, 0.95)
        p99 = percentile(latencies_sorted, 0.99)
        min_v = latencies_sorted[0]
        max_v = latencies_sorted[-1]
        print("--- Latency (ms) ---")
        print(f"min/avg/median/max: {min_v*1000:.2f} / {avg*1000:.2f} / "
              f"{p50*1000:.2f} / {max_v*1000:.2f}")
        if p95 is not None and p99 is not None:
            print(f"p95/p99: {p95*1000:.2f} / {p99*1000:.2f}")

    if elapsed > 0:
        throughput = total_ok / elapsed if expect_response else total_sent / elapsed
        print(f"Throughput: {throughput:.2f} req/s")

    return 0


def main() -> int:
    args = parse_args()
    try:
        return asyncio.run(main_async(args))
    except KeyboardInterrupt:
        return 130


if __name__ == "__main__":
    sys.exit(main())
