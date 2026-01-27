import requests
import json
import time
import concurrent.futures
import statistics

# Configuration
URL = "http://localhost:8081"
HEADERS = {'content-type': 'application/json'}

# Test Payload
def create_payload(id):
    return {
        "jsonrpc": "2.0",
        "method": "hello",
        "params": ["User" + str(id)],
        "id": str(id)
    }

def send_request(id):
    payload = create_payload(id)
    start_time = time.time()
    try:
        response = requests.post(URL, data=json.dumps(payload), headers=HEADERS)
        end_time = time.time()
        
        # Validation
        if response.status_code != 200:
            return None, f"Status {response.status_code}"
            
        json_resp = response.json()
        if "error" in json_resp:
             return None, f"RPC Error: {json_resp['error']}"
             
        expected_result = f"Hello, User{id}!"
        if json_resp.get("result") != expected_result:
            return None, f"Invalid Result: {json_resp.get('result')} != {expected_result}"
            
        return end_time - start_time, "OK"
    except Exception as e:
        return None, str(e)

def run_load_test(total_requests, concurrency):
    print(f"Starting load test: {total_requests} requests with {concurrency} concurrency...")
    
    results = []
    start_total = time.time()
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(send_request, i) for i in range(total_requests)]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())
            
    end_total = time.time()
    duration = end_total - start_total
    
    # Process results
    # Result format: (latency, status) or (None, error_msg)
    latencies = [r[0] * 1000 for r in results if r[0] is not None] # ms
    errors = [r[1] for r in results if r[0] is None]
    
    print(f"\n--- Results ---")
    print(f"Total Time: {duration:.2f} s")
    if duration > 0:
        print(f"Requests/sec (QPS): {len(latencies) / duration:.2f}")
    print(f"Avg Latency: {statistics.mean(latencies):.2f} ms" if latencies else "Avg Latency: N/A")
    print(f"P99 Latency: {statistics.quantiles(latencies, n=100)[98]:.2f} ms" if len(latencies) > 100 else "P99: N/A")
    print(f"Success Rate: {(len(latencies) / total_requests) * 100:.2f}%")
    
    if errors:
        print(f"Errors: {len(errors)}")
        # Print first few errors
        for e in errors[:5]:
            print(f" - {e}")

if __name__ == "__main__":
    # Ensure server is running before executing
    try:
        run_load_test(1000, 50)
    except Exception as e:
        print(f"Test failed: {e}")
