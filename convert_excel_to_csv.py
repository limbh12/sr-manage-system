import pandas as pd
import csv

def convert_excel_to_csv():
    input_file = 'docs/openapi_survey.xlsx'
    output_file = 'openapi_survey_result.csv'
    
    try:
        # Read Excel file with no header to access by index
        df = pd.read_excel(input_file, header=None)
        
        # Data starts from row 2 (0-based index)
        data_rows = df.iloc[2:]
        
        # Prepare the output data list
        output_data = []
        
        # Define the header for the CSV (from the template)
        headers = [
            "기관명", "부서", "담당자명", "연락처", "이메일", "수신파일명", "수신일자", "시스템명",
            "현행방식", "희망방식", "분산형희망사유", "유지관리운영", "유지관리장소", "유지관리주소", "유지관리비고",
            "운영환경", "서버위치", "WEB서버OS", "WEB서버OS종류", "WEB서버OS버전", "WEB서버종류", "WEB서버종류기타", "WEB서버버전",
            "WAS서버OS", "WAS서버OS종류", "WAS서버OS버전", "WAS서버종류", "WAS서버종류기타", "WAS서버버전",
            "DB서버OS", "DB서버OS종류", "DB서버OS버전", "DB서버종류", "DB서버종류기타", "DB서버버전",
            "개발언어", "개발언어기타", "개발언어버전", "개발프레임워크", "개발프레임워크기타", "개발프레임워크버전",
            "기타요청사항", "비고"
        ]
        
        for index, row in data_rows.iterrows():
            # Skip empty rows if any (check if '기관명' is empty)
            if pd.isna(row[2]):
                continue
                
            # Helper to safely get value
            def get_val(col_idx):
                val = row[col_idx]
                return str(val).strip() if pd.notna(val) else ""

            # Map columns
            item = {
                "기관명": get_val(2),
                "부서": get_val(5),
                "담당자명": get_val(4),
                "연락처": get_val(8),
                "이메일": get_val(7),
                "수신파일명": get_val(9),
                "수신일자": get_val(3),
                "시스템명": get_val(14),
                "현행방식": get_val(15),
                "희망방식": get_val(16),
                "분산형희망사유": get_val(17),
                "유지관리운영": get_val(18),
                "유지관리장소": get_val(19),
                "유지관리주소": get_val(20),
                "유지관리비고": get_val(21),
                "운영환경": get_val(25),
                "서버위치": get_val(26),
                "WEB서버OS": get_val(27),
                "WEB서버OS종류": get_val(28), # Combined in Excel
                "WEB서버OS버전": "", # Can't easily split without logic
                "WEB서버종류": get_val(29),
                "WEB서버종류기타": "",
                "WEB서버버전": get_val(30),
                "WAS서버OS": get_val(31),
                "WAS서버OS종류": get_val(32), # Combined in Excel
                "WAS서버OS버전": "",
                "WAS서버종류": get_val(33),
                "WAS서버종류기타": "",
                "WAS서버버전": get_val(34),
                "DB서버OS": "", # Not in Excel
                "DB서버OS종류": "",
                "DB서버OS버전": "",
                "DB서버종류": get_val(35),
                "DB서버종류기타": "",
                "DB서버버전": get_val(36),
                "개발언어": get_val(37),
                "개발언어기타": "",
                "개발언어버전": get_val(38),
                "개발프레임워크": get_val(39),
                "개발프레임워크기타": "",
                "개발프레임워크버전": get_val(40),
                "기타요청사항": get_val(46),
                "비고": get_val(47)
            }
            
            # Create a list in the order of headers
            row_data = [item[h] for h in headers]
            output_data.append(row_data)
            
        # Write to CSV
        with open(output_file, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.writer(f)
            writer.writerow(headers)
            writer.writerows(output_data)
            
        print(f"Successfully created {output_file} with {len(output_data)} rows.")
        
    except Exception as e:
        print(f"Error converting file: {e}")

if __name__ == "__main__":
    convert_excel_to_csv()
